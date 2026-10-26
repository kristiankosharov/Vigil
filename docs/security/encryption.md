# Encryption

How Vigil protects the PIN, the backend tokens and the documents on the device. The code is the
source of truth: `:common:security`, `:feature:applock:data:core`, `:feature:auth:data:core`
(`TokenStorageImpl`) and `:feature:documents:data:core`.

> [!WARNING]
> **TODO: not at fintech level yet.** Everything below runs on the phone alone. These gaps are known
> and deliberately left for later:
>
> **Needs the backend**
> - **PIN attempts are enforced only by the app.** On a rooted phone an attacker can restore an old
>   `applock_settings.json` to reset the counter, or hook the app and call the pepper key directly with
>   no limit (a 6-digit PIN then falls in about a day). Fix: the server holds part of the key and
>   rate-limits PIN checks, so the phone alone can't check a guess.
> - **Certificate pinning** for the backend (plus a `network_security_config` that forbids cleartext;
>   with `minSdk` 26, plain HTTP is still allowed by default on Android 8.x).
> - **Session tokens bound to the device:** tokens signed with a Keystore key (DPoP-style) with Key
>   Attestation, server-side revocation, step-up auth for sensitive actions. Also the Firebase refresh
>   token that still lives outside the vault (`TODO(auth)`, see "Backend tokens").
>
> **App side, later**
> - **Device integrity and root detection:** Play Integrity API, Key Attestation, root, hook (Frida)
>   and emulator detection, anti-tamper (RASP).
> - **Overlay and accessibility protection** on the PIN screen and other sensitive screens:
>   `Window.setHideOverlayWindows(true)` (Android 12+), `View.setAccessibilityDataSensitive` (Android
>   14+), and ignoring touches while the window is covered (`filterTouchesWhenObscured`).

## Key hierarchy

```mermaid
flowchart TD
    pin["PIN"] --> kdf["Argon2id + pepper<br/>(Keystore HMAC key)"]
    finger["Fingerprint"] --> bio["Biometric key<br/>(Keystore, every use)"]
    kdf -- unwraps --> vault["Vault key<br/>(AES-256, memory only)"]
    bio -- unwraps --> vault
    vault -- seals --> tokens["Access tokens<br/>auth_session.json"]
    vault -- seals --> index["Document index<br/>titles, scanned text"]
    vault -- seals --> wrapped["Wrapped document key"]
    rsa["RSA-3072 key<br/>(Keystore, OAEP)"] -- unwraps --> dek
    wrapped --> dek["Document key<br/>(AES-256, one per document)"]
    dek -- encrypts --> pages["Pages<br/>encrypted files"]

    classDef keystore fill:#E1F5EE,stroke:#0F6E56,color:#085041
    classDef secret fill:#EEEDFE,stroke:#534AB7,color:#3C3489
    classDef disk fill:#F1EFE8,stroke:#5F5E5A,color:#444441
    class kdf,bio,rsa keystore
    class pin,finger,vault,dek secret
    class tokens,index,wrapped,pages disk
```

Teal: keys inside the Android Keystore (they never leave secure hardware). Purple: secrets and keys
that exist only in memory. Gray: what is stored on disk, always encrypted.

Every DataStore file (`applock_settings.json`, `auth_session.json`, `documents_index.json`, …) is also
encrypted as a whole by Tink with a Keystore-backed AES-256-GCM key (`:common:datastore`). That outer
layer is not drawn: it protects the files at rest, but it opens without the PIN.

## The PIN and the vault key

The PIN is never stored, not even as a hash. It is part of the key that decrypts everything else.

1. **PIN key:** `HMAC-SHA256(pepper, Argon2id(PIN, salt))` (`PinKeyDerivation`).
   - Argon2id (Bouncy Castle) with the OWASP baseline: 19 MiB, 2 passes, 1 lane. The cost is stored
     with each wrapped key (`KdfParams`), so it can be raised later.
   - The pepper is a non-exportable Keystore HMAC key (`vault_pin_pepper_v1`, StrongBox when the
     phone has one, else the TEE). A guess can only be checked on this phone: copying the files
     somewhere else gives an attacker nothing to brute-force.
2. **Vault key:** a random AES-256 key, stored only as `AES-GCM(PIN key, vault key)` in
   `applock_settings.json`.
   - A PIN is right exactly when that decrypts (the GCM tag matches).
   - While unlocked the key sits in memory (`Vault`); locking wipes it.
3. **Fingerprint:** a second copy of the vault key, encrypted by a Keystore AES-GCM key that works only
   right after a strong-biometric check (`vault_biometric_v1`). BiometricPrompt always gets a
   `CryptoObject`, so the fingerprint really decrypts the key instead of only saying "yes". Adding or
   removing a fingerprint destroys that key; fingerprint unlock then turns itself off and the PIN
   still works.

```mermaid
sequenceDiagram
    participant U as User
    participant A as App lock
    participant K as Keystore
    participant V as Vault
    U->>A: enters the PIN
    A->>A: count the attempt first
    A->>A: Argon2id(PIN, salt)
    A->>K: HMAC with the pepper key
    K-->>A: PIN key
    A->>A: AES-GCM open(PIN key, wrapped vault key)
    alt tag matches
        A->>V: open(vault key)
        A->>A: reset the attempt count
    else wrong PIN
        A->>A: wait 30 s … 15 min, or erase after the 10th
    end
```

### Wrong PINs

- 5 free tries, then waits of 30 s, 1 min, 5 min and 15 min (`PinAttemptPolicy`).
- The 10th wrong PIN erases every user's data (`UserDataCleaner`s, the biometric key, the documents'
  RSA key) and signs out.
- The count is written **before** each check, so killing the app mid-check never gives a free try.
- Waits use the time since boot, not the wall clock; after a reboot the full wait applies again.

### Other PIN rules

- Easy PINs are refused on Create: one repeated digit, runs up or down, well-known PINs, anything
  that reads as a date (`PinRules.isWeak`).
- PINs live in `CharArray`s that are wiped after use (`PinBuffer`), never in `String`s or saved state.
- The whole app has `FLAG_SECURE`: no screenshots, screen recording or recents thumbnail.

## Backend tokens

`TokenStorageImpl` seals the tokens with the vault key (associated data `vigil.auth.tokens`), so the
app has no tokens while it is locked. Sign-in happens before the PIN exists: tokens from then wait in
memory and are sealed once the PIN is created. If the app dies first, the user signs in again.

Known gap: Firebase keeps its own refresh token outside the vault. Once the backend exists, the app
exchanges the Firebase ID token for backend tokens and then signs out of Firebase (`TODO(auth)`).

## Documents (envelope encryption)

Each document has its own random AES-256 **document key**.

| What | Encrypted with | Stored in |
|---|---|---|
| Pages | AES-GCM with the document key; associated data `vigil.documents.page:<id>:<page>` | `noBackupFilesDir/documents/<id>/<page>.bin` |
| Document key | RSA-OAEP with the Keystore key `documents_kek_v1`, then AES-GCM with the vault key | `documents_index.json` |
| Title, status, page count, scanned text | AES-GCM with the vault key; associated data `vigil.documents.metadata:<id>` | `documents_index.json` |

```mermaid
sequenceDiagram
    participant R as Documents repository
    participant V as Vault
    participant K as Keystore (RSA)
    participant F as Files
    Note over R: Add a document
    R->>R: new random document key
    R->>F: AES-GCM(document key, each page)
    R->>K: RSA-OAEP wrap(document key)
    R->>V: seal(wrapped key), seal(metadata)
    Note over R: Open a page
    R->>V: unseal(wrapped key)
    R->>K: RSA-OAEP unwrap
    K-->>R: document key
    R->>F: read and AES-GCM open the page
```

- **Two locks on every document:** opening a page needs both the Keystore (RSA private key, which
  never leaves it) and an unlocked app (the vault key).
- **Bound to its place:** every sealed value carries its document id (and page number) as associated
  data, so pages and keys can't be swapped between documents.
- **Delete = forget the key:** deleting a document removes its wrapped key first; from then on its
  pages can never be decrypted, even if deleting the files fails.
- **Key rotation:** a new RSA key gets a new alias (`documents_kek_v2`). Rotating re-wraps only the
  small document keys; the page files stay as they are.
- **No backups:** the pages are in `noBackupFilesDir` and the DataStore files are excluded from
  backup. They couldn't be decrypted on another phone anyway.
- **RSA details:** RSA-3072 in the TEE (StrongBox only guarantees RSA-2048). OAEP uses SHA-256 with
  MGF1 SHA-1, the only MGF1 digest the Keystore accepts before Android 15; that is still sound for
  OAEP. Wrapping uses a software copy of the public key, so it needs no Keystore call.

## What erases what

| Event | Effect |
|---|---|
| App locked (start, 5 s in the background, "Lock wallet now") | Vault key wiped from memory: no tokens, no document list, no pages. The 5 s timer runs while the app is in the background, so the key doesn't wait in memory until the user returns; if Android froze the process, it locks on return. |
| 10th wrong PIN | Every `UserDataCleaner` runs, the biometric key is deleted, the user is signed out |
| Logout | Every `UserDataCleaner` runs: tokens, PIN wrap, document index, pages and the documents' RSA key |
| Fingerprints added or removed | Biometric key destroyed; fingerprint unlock turns off, the PIN still works |
