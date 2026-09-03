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
