import fs from 'node:fs';
import path from 'node:path';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const siteUrl = (env.VITE_SITE_URL || 'http://localhost:5173').replace(/\/$/, '');
  const ymId = (env.VITE_YM_ID || '').trim();
  const ymIdValid = /^\d+$/.test(ymId);

  return {
    plugins: [
      react(),
      {
        name: 'seo-html-and-files',
        transformIndexHtml(html) {
          let out = html.replaceAll('__SITE_URL__', siteUrl);
          if (ymIdValid) {
            out = out.replace(
              '<!-- __YM_NOSCRIPT__ -->',
              `<noscript><div><img src="https://mc.yandex.ru/watch/${ymId}" width="1" height="1" style="position:absolute;left:-9999px" alt="" /></div></noscript>`
            );
          } else {
            out = out.replace('<!-- __YM_NOSCRIPT__ -->', '');
          }
          return out;
        },
        closeBundle() {
          const outDir = path.resolve(process.cwd(), 'dist');
          const robots = [
            'User-agent: *',
            'Allow: /',
            '',
            `Host: ${siteUrl.replace(/^https?:\/\//, '')}`,
            '',
            `Sitemap: ${siteUrl}/sitemap.xml`,
            ''
          ].join('\n');
          const sitemap = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>${siteUrl}/</loc>
    <changefreq>weekly</changefreq>
    <priority>1.0</priority>
  </url>
</urlset>
`;
          try {
            fs.writeFileSync(path.join(outDir, 'robots.txt'), robots, 'utf8');
            fs.writeFileSync(path.join(outDir, 'sitemap.xml'), sitemap, 'utf8');
          } catch (e) {
            console.warn('seo-html-and-files: could not write robots/sitemap', e);
          }
        }
      }
    ],
    server: {
      port: 5173,
      host: '0.0.0.0'
    }
  };
});
