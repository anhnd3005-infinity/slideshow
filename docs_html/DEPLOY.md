# 🚀 Quick Deploy Guide

## Deploy lên Vercel

### Cách 1: Vercel Dashboard (Dễ nhất)

1. Vào https://vercel.com
2. Login với GitHub
3. Click "Add New Project"
4. Chọn repo từ GitHub
5. **Root Directory**: `docs_html`
6. **Framework**: Other
7. Click "Deploy"

### Cách 2: Vercel CLI

```bash
npm i -g vercel
cd docs_html
vercel
```

### Cách 3: GitHub Integration

1. Connect repo trong Vercel
2. Set Root Directory = `docs_html`
3. Auto-deploy mỗi khi push

---

## ⚡ Quick Commands

```bash
# Sync markdown files
cd docs_html && ./sync_markdown.sh

# Test local
python3 -m http.server 8000

# Deploy
vercel
```

---

## 📋 Files Cần Có

- ✅ `index.html` - Main file
- ✅ `styles.css` - Styles
- ✅ `app.js` - Logic
- ✅ `vercel.json` - Config
- ✅ `*.md` - Markdown files

Tất cả đã có sẵn! 🎉

