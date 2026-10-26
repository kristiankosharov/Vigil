/*
 * Minimal renderer for the design mock-ups in this folder (*.dc.html), so they open in any browser
 * without the original design tool. It supports only what the screens use:
 *   - {{expr}} holes in text and attributes
 *   - <sc-for list="{{items}}" as="item"> and <sc-if value="{{flag}}">
 *   - onClick / onInput / onChange handlers bound to functions
 *   - a `class Component extends DCLogic` script with state, setState() and renderVals()
 *   - props with defaults from data-props
 * It also renders every everyday screen in light or dark (see "Light / dark" below).
 */
(function () {
  'use strict';

  var EVENTS = { onclick: 'click', oninput: 'input', onchange: 'change' };
  var HOLE = /\{\{([\s\S]+?)\}\}/g;
  var ONLY_HOLE = /^\s*\{\{([\s\S]+?)\}\}\s*$/;

  function evaluate(expr, scope) {
    // Expressions are trusted: they come from the mock-up files in this folder.
    return new Function('scope', 'with (scope) { return (' + expr + '); }')(scope);
  }

  function interpolate(text, scope) {
    return text.replace(HOLE, function (_, expr) {
      var v = evaluate(expr, scope);
      return v === undefined || v === null ? '' : String(v);
    });
  }

  function renderChildren(node, scope, out) {
    for (var c = node.firstChild; c; c = c.nextSibling) renderNode(c, scope, out);
  }

  function renderNode(node, scope, out) {
    if (node.nodeType === Node.TEXT_NODE) {
      out.appendChild(document.createTextNode(interpolate(node.nodeValue, scope)));
      return;
    }
    if (node.nodeType !== Node.ELEMENT_NODE) return;
    var tag = node.tagName.toLowerCase();

    if (tag === 'sc-for') {
      var list = evaluate(node.getAttribute('list').match(ONLY_HOLE)[1], scope) || [];
      var as = node.getAttribute('as');
      list.forEach(function (item) {
        var inner = Object.create(scope);
        inner[as] = item;
        renderChildren(node, inner, out);
      });
      return;
    }
    if (tag === 'sc-if') {
      if (evaluate(node.getAttribute('value').match(ONLY_HOLE)[1], scope)) renderChildren(node, scope, out);
      return;
    }

    var el = node.namespaceURI === 'http://www.w3.org/2000/svg'
      ? document.createElementNS(node.namespaceURI, node.tagName)
      : document.createElement(node.tagName);
    Array.prototype.forEach.call(node.attributes, function (attr) {
      var name = attr.name;
      if (name.indexOf('hint-') === 0) return;
      var only = attr.value.match(ONLY_HOLE);
      if (EVENTS[name.toLowerCase()] && only) {
        var fn = evaluate(only[1], scope);
        if (typeof fn === 'function') el.addEventListener(EVENTS[name.toLowerCase()], fn);
        return;
      }
      if ((name === 'checked' || name === 'value') && only) {
        var v = evaluate(only[1], scope);
        if (name === 'checked') el.checked = !!v;
        else el.value = v === undefined || v === null ? '' : String(v);
        if (name === 'value') el.setAttribute('value', el.value);
        return;
      }
      el.setAttribute(name, interpolate(attr.value, scope));
    });
    renderChildren(node, scope, el);
    out.appendChild(el);
  }

  function readProps(script) {
    var defs = JSON.parse(script.getAttribute('data-props') || '{}');
    var props = {};
    Object.keys(defs).forEach(function (key) {
      if (key.charAt(0) === '$') return;
      var d = defs[key];
      props[key] = d && typeof d === 'object' && 'default' in d ? d['default'] : d;
    });
    return props;
  }

  // ---- Light / dark ---------------------------------------------------------------------------
  // The mock-ups are written in the light scheme. When the chosen theme is dark, every light role
  // color is swapped for its dark role (docs/design/README.md, Color) before rendering. The choice
  // ("system", "light" or "dark") is shared by all screens through localStorage; ?theme= overrides it.

  var THEME_KEY = 'vigil-theme';
  // Already dark (or showing both themes): never remapped.
  var FIXED_THEME = ['CreatePin', 'ConfirmPin', 'EnableBiometric', 'Main', 'ChangePin', 'Scan', 'CompSecurity', 'Theme'];

  var BG = {
    '#F9F9FF': '#111318', '#FFFFFF': '#1D2024', '#F3F3FA': '#191C20', '#EDEDF4': '#282A2F',
    '#E7E8EE': '#282A2F', '#E2E2E9': '#33353A', '#D9D9E0': '#33353A', '#415F91': '#AAC7FF',
    '#546F9C': '#9AB8EF', '#D6E3FF': '#284777', '#2E3036': '#E2E2E9', '#191C20': '#E2E2E9',
    '#FFDAD6': '#93000A', '#FEF0C7': '#7A2E0E', '#DCFAE6': '#074D31', '#BA1A1A': '#FFB4AB',
    '#B54708': '#FDB022', '#067647': '#47CD89', '#74777F': '#8E9099', '#C4C6D0': '#44474E',
  };
  var FG = {
    '#191C20': '#E2E2E9', '#44474E': '#C4C6D0', '#74777F': '#8E9099', '#415F91': '#AAC7FF',
    '#284777': '#D6E3FF', '#FFFFFF': '#111318', '#F0F0F7': '#2E3036', '#93370D': '#FEF0C7',
    '#085D3A': '#DCFAE6', '#93000A': '#FFDAD6', '#BA1A1A': '#FFB4AB', '#B54708': '#FDB022',
    '#067647': '#47CD89', '#C4C6D0': '#44474E',
  };
  var LINE = {
    '#E2E2E9': '#33353A', '#C4C6D0': '#44474E', '#74777F': '#8E9099', '#EDEDF4': '#282A2F',
    '#F3F3FA': '#191C20', '#FFFFFF': '#1D2024', '#F9F9FF': '#111318', '#BA1A1A': '#FFB4AB',
    '#415F91': '#AAC7FF', '#FFDAD6': '#93000A', '#2E3036': '#E2E2E9', '#191C20': '#E2E2E9',
    '#D6E3FF': '#284777',
  };
  // Status colors that live in plain string literals in the screens' logic (corner icons).
  var LITERALS = { '#B54708': '#FDB022', '#067647': '#47CD89', '#74777F': '#8E9099' };

  function chosenTheme() {
    var param = new URLSearchParams(location.search).get('theme');
    if (param === 'light' || param === 'dark' || param === 'system') return param;
    try {
      return localStorage.getItem(THEME_KEY) || 'system';
    } catch (e) {
      return 'system'; // storage blocked: follow the system
    }
  }

  function isDark() {
    var theme = chosenTheme();
    if (theme === 'system') return !!(window.matchMedia && matchMedia('(prefers-color-scheme: dark)').matches);
    return theme === 'dark';
  }

  function screenName() {
    return decodeURIComponent(location.pathname.split('/').pop() || '').replace('.dc.html', '');
  }

  function mapHex(value, table) {
    return value
      .replace(/#[0-9A-Fa-f]{6}\b/g, function (h) { return table[h.toUpperCase()] || h; })
      .replace(/rgba\(25, 28, 32, (0?\.\d+)\)/g, 'rgba(226, 226, 233, $1)');
  }

  function toDark(text) {
    return text
      // CSS declarations, in style attributes, <style> blocks and style strings in the logic.
      .replace(
        /(^|[;{\s"'`])(background(?:-color)?|border(?:-[a-z]+)?|outline(?:-color)?|color|fill|stroke|accent-color)\s*:\s*([^;"'`}]*)/g,
        function (_, pre, prop, value) {
          var table = prop.indexOf('background') === 0 ? BG : (/^(border|outline)/.test(prop) ? LINE : FG);
          return pre + prop + ': ' + mapHex(value, table);
        })
      // SVG presentation attributes.
      .replace(/\b(fill|stroke)="(#[0-9A-Fa-f]{6})"/g, function (_, attr, h) {
        return attr + '="' + (FG[h.toUpperCase()] || h) + '"';
      })
      .replace(/'(#[0-9A-Fa-f]{6})'/g, function (m, h) {
        var mapped = LITERALS[h.toUpperCase()];
        return mapped ? "'" + mapped + "'" : m;
      });
  }

  var darkActive = isDark() && FIXED_THEME.indexOf(screenName()) < 0;

  window.vigilTheme = chosenTheme;
  window.vigilSetTheme = function (theme) {
    try {
      localStorage.setItem(THEME_KEY, theme);
    } catch (e) { /* storage blocked: the change lasts until reload */ }
    if (isDark() !== darkActive && FIXED_THEME.indexOf(screenName()) < 0) location.reload();
  };
  // Another screen (or the index) changed the theme: re-render this one in it.
  window.addEventListener('storage', function (e) {
    if (e.key === THEME_KEY) location.reload();
  });
  if (window.matchMedia) {
    matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function () {
      if (chosenTheme() === 'system') location.reload();
    });
  }

  function DCLogic(props) {
    this.props = props || {};
    this.state = {};
  }
  DCLogic.prototype.setState = function (patch) {
    this.state = Object.assign({}, this.state, typeof patch === 'function' ? patch(this.state) : patch);
    if (this.__render) this.__render();
  };
  DCLogic.prototype.renderVals = function () {
    return {};
  };
  window.DCLogic = DCLogic;

  function mount() {
    var host = document.querySelector('x-dc');
    if (!host) return;

    if (darkActive) host.innerHTML = toDark(host.innerHTML);
    document.documentElement.style.background = darkActive ? '#111318' : '';

    var helmet = host.querySelector('helmet');
    if (helmet) {
      while (helmet.firstChild) document.head.appendChild(helmet.firstChild);
      helmet.remove();
    }

    var template = document.createElement('div');
    while (host.firstChild) template.appendChild(host.firstChild);
    var root = document.createElement('div');
    host.replaceWith(root);

    var script = document.querySelector('script[data-dc-script]');
    var logic = script ? (darkActive ? toDark(script.textContent) : script.textContent) : '';
    var Component = script ? new Function('DCLogic', logic + '\nreturn Component;')(DCLogic) : DCLogic;
    var component = new Component(script ? readProps(script) : {});

    component.__render = function () {
      // Keep focus and caret in a text input across re-renders (search, forms).
      var active = document.activeElement;
      var label = active && active.tagName === 'INPUT' ? active.getAttribute('aria-label') : null;
      var caret = label ? active.selectionStart : null;

      var out = document.createDocumentFragment();
      renderChildren(template, component.renderVals() || {}, out);
      root.replaceChildren(out);

      if (label) {
        var input = root.querySelector('input[aria-label="' + label + '"]');
        if (input) {
          input.focus();
          try { input.setSelectionRange(caret, caret); } catch (e) { /* not a text input */ }
        }
      }
    };
    component.__render();
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', mount);
  else mount();
})();
