import fs from 'node:fs';
import path from 'node:path';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const siteUrl = (env.VITE_SITE_URL || 'http://localhost:5173').replace(/\/$/, '');

  return {
    plugins: [
      react(),
      {
        name: 'seo-html-and-files',
        transformIndexHtml(html) {
          return html.replaceAll('__SITE_URL__', siteUrl);
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
