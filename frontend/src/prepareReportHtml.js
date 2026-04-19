import DOMPurify from 'isomorphic-dompurify';
import { marked } from 'marked';

marked.use({
  gfm: true,
  breaks: true
});

const SANITIZE = {
  USE_PROFILES: { html: true },
  ADD_TAGS: ['section', 'article', 'main', 'figure', 'figcaption'],
  ADD_ATTR: ['target', 'rel', 'class', 'id', 'style']
};

let hooksInstalled = false;

function ensureLinkHooks() {
  if (hooksInstalled) {
    return;
  }
  hooksInstalled = true;
  DOMPurify.addHook('afterSanitizeAttributes', (node) => {
    if (node.tagName === 'A' && node.hasAttribute('href')) {
      node.setAttribute('target', '_blank');
      node.setAttribute('rel', 'noopener noreferrer');
    }
  });
}

/**
 * Ответ модели: чаще Markdown (GFM), иногда HTML. Приводим к безопасному HTML для вставки в DOM.
 */
export function prepareReportHtml(raw) {
  ensureLinkHooks();
  if (raw == null) {
    return '';
  }
  const text = String(raw).trim();
  if (!text) {
    return '';
  }

  const html = looksLikeHtmlFragment(text) ? text : marked.parse(text);

  return DOMPurify.sanitize(html, SANITIZE);
}

function looksLikeHtmlFragment(s) {
  const t = s.trimStart();
  if (!t.startsWith('<')) {
    return false;
  }
  // GFM autolink <https://...> — это Markdown, не HTML
  if (/^<[a-z][a-z+.-]*:\/\//i.test(t)) {
    return false;
  }
  return /^<[a-zA-Z!?/]/.test(t);
}
