/*
 * Minimal renderer for the design mock-ups in this folder (*.dc.html), so they open in any browser
 * without the original design tool. It supports only what the screens use:
 *   - {{expr}} holes in text and attributes
 *   - <sc-for list="{{items}}" as="item"> and <sc-if value="{{flag}}">
 *   - onClick / onInput / onChange handlers bound to functions
 *   - a `class Component extends DCLogic` script with state, setState() and renderVals()
 *   - props with defaults from data-props
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
    var Component = script
      ? new Function('DCLogic', script.textContent + '\nreturn Component;')(DCLogic)
      : DCLogic;
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
