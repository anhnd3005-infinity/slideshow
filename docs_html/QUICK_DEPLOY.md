# ⚡ Quick Deploy Guide

## 🚀 Push lên GitHub + Deploy Vercel

### Bước 1: Setup GitHub Remote (nếu chưa có)

```bash
# Kiểm tra remote hiện tại
git remote -v

# Thêm GitHub remote (thay YOUR_USERNAME và YOUR_REPO)
git remote add github https://github.com/YOUR_USERNAME/YOUR_REPO.git
```

### Bước 2: Chạy Script Deploy

```bash
# Script tự động sync, commit và push
./deploy_to_github.sh
```

Hoặc làm thủ công:

```bash
# 1. Sync markdown files
cd docs_html && ./sync_markdown.sh && cd ..

# 2. Add files
git add docs_html/ *.md DEPLOY_GUIDE.md

# 3. Commit
git commit -m "docs: Add architecture documentation and HTML viewer"

# 4. Push
git push github master
# hoặc
git push github main
```

---

## 🌐 Deploy lên Vercel

### Cách 1: Vercel Dashboard (Dễ nhất) ⭐

1. **Truy cập**: https://vercel.com
2. **Login** với GitHub
3. **Import Project**:
   - Click "Add New Project"
   - Chọn repository từ GitHub
   - **Root Directory**: `docs_html` ⚠️ **QUAN TRỌNG**
   - **Framework Preset**: Other
   - **Build Command**: (để trống)
   - **Output Directory**: `.`
4. **Click "Deploy"**

### Cách 2: Vercel CLI

```bash
# Install Vercel CLI
npm i -g vercel

# Login
vercel login

# Deploy
cd docs_html
vercel

# Follow prompts
```

---

## ✅ Checklist

- [ ] ✅ Đã sync markdown: `cd docs_html && ./sync_markdown.sh`
- [ ] ✅ Đã add GitHub remote
- [ ] ✅ Đã commit và push lên GitHub
- [ ] ✅ Đã setup Vercel project
- [ ] ✅ Đã set **Root Directory = `docs_html`** ⚠️
- [ ] ✅ Đã deploy thành công

---

## 🔗 Sau Khi Deploy

Vercel sẽ cung cấp URL:
- `https://your-project.vercel.app`

Bạn có thể:
- ✅ Share với team
- ✅ Add custom domain
- ✅ Auto-deploy từ GitHub

---

## 🐛 Troubleshooting

**Markdown không load?**
→ Check Root Directory = `docs_html` trong Vercel settings

**404 Not Found?**
→ Check `vercel.json` có đúng không

**CORS Error?**
→ Vercel đã handle tự động, nếu vẫn lỗi check headers trong `vercel.json`

---

## 📞 Need Help?

Check `DEPLOY_GUIDE.md` để xem hướng dẫn chi tiết hơn.

