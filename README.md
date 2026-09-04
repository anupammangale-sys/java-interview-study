# Java Interview Study

The published site. **Everything in here is generated. Do not edit it by hand.**

The source lives in `study/` in the working project, and this folder is produced by:

```bash
python build-site.py
```

That script reads the episode plan and the registry of built episodes, generates the
index, and wraps each episode page in a real HTML document. The pages are authored for
the Claude artifact host, which supplies the doctype, charset and viewport meta itself,
so serving them raw would leave them with no viewport meta and render at desktop width
on a phone. The build adds those, plus a link back to the index.

## What is in here

- `epNN.html` one page per published episode, wrapped from `study/epNN.html`
- `code/epNN.html` the runnable demos behind that episode, highlighted at build time
- `code/raw/epNN/` the same files raw, one URL each, served as plain text
- `code/epNN.zip` all of them, ready to run
- `index.html`, `site.css`, `netlify.toml`, `robots.txt`

Heap dumps and anything over 512 KB are never published. The build reports what it left out.

## Deploying

Netlify builds from this repository. Pushing to the default branch deploys.

```bash
python build-site.py
cd site
git add -A
git commit -m "Publish episode NN"
git push
```

No build command runs on Netlify. The generated files are committed as-is and the
publish directory is the repository root, so a deploy is only ever a file copy and
cannot fail on a build environment difference.
