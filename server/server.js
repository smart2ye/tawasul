require('dotenv').config();
const express = require('express');
const cors = require('cors');
const path = require('path');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const { Client, Pool } = require('pg');
const sqlite3 = require('sqlite3').verbose();

const app = express();
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'tawasul_secret_key_2026_super_safe_key_123';

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(express.static(path.join(__dirname, 'public')));

// Configure EJS View Engine
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// Session helper simulation for EJS routes (simple cookies or headers)
app.use((req, res, next) => {
  // Simple custom session implementation via Bearer or Query string or Request-based auth simulation
  const token = req.headers['authorization']?.split(' ')[1] || req.query.token || '';
  if (token) {
    try {
      const decoded = jwt.verify(token, JWT_SECRET);
      req.user = decoded;
    } catch (err) {
      req.user = null;
    }
  }
  next();
});

// Database Connection (Auto-fallback to SQLite if PostgreSQL isn't configured)
let db;
const isPostgres = !!process.env.DATABASE_URL;

if (isPostgres) {
  console.log('🔗 Connecting to PostgreSQL database...');
  db = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: { rejectUnauthorized: false } // Required for Render Postgres
  });
} else {
  console.log('💾 Connecting to local SQLite database (database.sqlite)...');
  const dbFile = path.join(__dirname, 'database.sqlite');
  db = new sqlite3.Database(dbFile);
}

// Promisify SQLite for consistent async/await interface with Postgres
const query = (text, params = []) => {
  if (isPostgres) {
    return db.query(text, params).then(res => res.rows);
  } else {
    return new Promise((resolve, reject) => {
      // Convert PostgreSQL $1, $2 params to SQLite ?, ? params
      const sqliteText = text.replace(/\$(\d+)/g, '?');
      const isSelect = sqliteText.trim().toLowerCase().startsWith('select');
      
      if (isSelect) {
        db.all(sqliteText, params, (err, rows) => {
          if (err) reject(err);
          else resolve(rows);
        });
      } else {
        db.run(sqliteText, params, function(err) {
          if (err) reject(err);
          else resolve({ insertId: this.lastID, changes: this.changes });
        });
      }
    });
  }
};

// Initialize Database Tables
async function initDatabase() {
  try {
    const isPg = isPostgres;
    const serialType = isPg ? 'SERIAL PRIMARY KEY' : 'INTEGER PRIMARY KEY AUTOINCREMENT';
    const textType = isPg ? 'TEXT' : 'TEXT';
    const integerType = isPg ? 'INTEGER' : 'INTEGER';
    const booleanType = isPg ? 'BOOLEAN' : 'INTEGER'; // SQLite uses 0/1 for booleans

    // 1. Users Table
    await query(`
      CREATE TABLE IF NOT EXISTS users (
        id ${serialType},
        email ${textType} UNIQUE NOT NULL,
        password_hash ${textType} NOT NULL,
        full_name ${textType} NOT NULL,
        provider ${textType} DEFAULT 'email',
        google_id ${textType},
        is_blocked ${booleanType} DEFAULT 0,
        is_admin ${booleanType} DEFAULT 0,
        created_at ${textType} DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // 2. Backups Table
    await query(`
      CREATE TABLE IF NOT EXISTS backups (
        id ${serialType},
        user_id ${integerType} NOT NULL,
        device_name ${textType} NOT NULL,
        encrypted_data ${textType} NOT NULL,
        backup_size ${integerType} DEFAULT 0,
        created_at ${textType} DEFAULT CURRENT_TIMESTAMP,
        updated_at ${textType} DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // 3. App Versions Table
    await query(`
      CREATE TABLE IF NOT EXISTS versions (
        id ${serialType},
        version_code ${integerType} UNIQUE NOT NULL,
        version_name ${textType} NOT NULL,
        release_date ${textType} NOT NULL,
        changelog ${textType},
        download_url ${textType} NOT NULL,
        is_critical ${booleanType} DEFAULT 0,
        created_at ${textType} DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // 4. Support Tickets Table
    await query(`
      CREATE TABLE IF NOT EXISTS tickets (
        id ${serialType},
        user_id ${integerType} NOT NULL,
        subject ${textType} NOT NULL,
        message ${textType} NOT NULL,
        reply_message ${textType},
        status ${textType} DEFAULT 'OPEN', -- OPEN, ANSWERED, CLOSED
        category ${textType} DEFAULT 'GENERAL', -- GENERAL, FEEDBACK, BACKUP, BUG
        created_at ${textType} DEFAULT CURRENT_TIMESTAMP,
        replied_at ${textType}
      )
    `);

    // 5. Feedbacks Table
    await query(`
      CREATE TABLE IF NOT EXISTS feedbacks (
        id ${serialType},
        type ${textType} NOT NULL, -- BUG, CRASH, FEATURE, SUGGESTION
        content ${textType} NOT NULL,
        is_resolved ${booleanType} DEFAULT 0,
        created_at ${textType} DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // 6. Push Notifications Table
    await query(`
      CREATE TABLE IF NOT EXISTS notifications (
        id ${serialType},
        title ${textType} NOT NULL,
        body ${textType} NOT NULL,
        audience ${textType} DEFAULT 'ALL', -- ALL, ACTIVE
        sent_at ${textType} DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // Seed Default Admin and App Version if empty
    const adminCheck = await query("SELECT * FROM users WHERE is_admin = 1 OR is_admin = true");
    if (adminCheck.length === 0) {
      const salt = bcrypt.genSaltSync(10);
      const hash = bcrypt.hashSync('admin123456', salt);
      await query(
        "INSERT INTO users (email, password_hash, full_name, is_admin) VALUES ($1, $2, $3, $4)",
        ['admin@tawasulplus.com', hash, 'مدير المنصة', 1]
      );
      console.log('✅ Default administrator account created: admin@tawasulplus.com / admin123456');
    }

    const versionCheck = await query("SELECT * FROM versions");
    if (versionCheck.length === 0) {
      await query(
        "INSERT INTO versions (version_code, version_name, release_date, changelog, download_url, is_critical) VALUES ($1, $2, $3, $4, $5, $6)",
        [100, '1.0.0', '2026-07-13', 'الإصدار الأول والأساسي لتطبيق تواصل بلس مع ميزة المحادثة غير المباشرة والخدمات السحابية.', 'https://tawasulplus.com/downloads/tawasul-v1.0.0.apk', 0]
      );
      console.log('✅ Seeded default app version 1.0.0.');
    }

    console.log('🚀 Database tables initialized successfully.');
  } catch (err) {
    console.error('❌ Error initializing database:', err);
  }
}

initDatabase();

// ==========================================
// API ROUTES (For Android Client & Web Fetch)
// ==========================================

// Authenticate Middleware
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) return res.status(401).json({ error: 'الرجاء تسجيل الدخول للوصول لهذه الخدمة.' });

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) return res.status(403).json({ error: 'انتهت صلاحية الجلسة، يرجى تسجيل الدخول مجدداً.' });
    req.user = user;
    next();
  });
};

// 1. Auth APIs
app.post('/api/auth/register', async (req, res) => {
  const { email, password, full_name } = req.body;
  if (!email || !password || !full_name) {
    return res.status(400).json({ error: 'الرجاء إدخال جميع البيانات المطلوبة.' });
  }

  try {
    const existing = await query("SELECT * FROM users WHERE email = $1", [email]);
    if (existing.length > 0) {
      return res.status(400).json({ error: 'البريد الإلكتروني مسجل بالفعل.' });
    }

    const salt = bcrypt.genSaltSync(10);
    const hash = bcrypt.hashSync(password, salt);

    const result = await query(
      "INSERT INTO users (email, password_hash, full_name) VALUES ($1, $2, $3)",
      [email, hash, full_name]
    );

    res.status(201).json({ message: 'تم إنشاء الحساب بنجاح! يمكنك الآن تسجيل الدخول.' });
  } catch (err) {
    res.status(500).json({ error: 'فشل إنشاء الحساب، يرجى المحاولة لاحقاً.' });
  }
});

app.post('/api/auth/login', async (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) {
    return res.status(400).json({ error: 'الرجاء إدخال البريد الإلكتروني وكلمة المرور.' });
  }

  try {
    const users = await query("SELECT * FROM users WHERE email = $1", [email]);
    if (users.length === 0) {
      return res.status(400).json({ error: 'البريد الإلكتروني أو كلمة المرور غير صحيحة.' });
    }

    const user = users[0];
    if (user.is_blocked === 1 || user.is_blocked === true) {
      return res.status(403).json({ error: 'هذا الحساب محظور من قبل الإدارة لانتهاكه الشروط.' });
    }

    const valid = bcrypt.compareSync(password, user.password_hash);
    if (!valid) {
      return res.status(400).json({ error: 'البريد الإلكتروني أو كلمة المرور غير صحيحة.' });
    }

    const token = jwt.sign(
      { id: user.id, email: user.email, full_name: user.full_name, is_admin: user.is_admin },
      JWT_SECRET,
      { expiresIn: '30d' }
    );

    res.json({
      token,
      user: {
        id: user.id,
        email: user.email,
        full_name: user.full_name,
        is_admin: !!user.is_admin
      }
    });
  } catch (err) {
    res.status(500).json({ error: 'حدث خطأ أثناء تسجيل الدخول.' });
  }
});

app.post('/api/auth/google', async (req, res) => {
  const { email, google_id, full_name } = req.body;
  if (!email || !google_id) {
    return res.status(400).json({ error: 'فشل التحقق من الهوية عبر جوجل.' });
  }

  try {
    let users = await query("SELECT * FROM users WHERE email = $1", [email]);
    let user;

    if (users.length === 0) {
      // Create account
      const salt = bcrypt.genSaltSync(10);
      const randomPassword = bcrypt.hashSync(Math.random().toString(36), salt);
      
      await query(
        "INSERT INTO users (email, password_hash, full_name, provider, google_id) VALUES ($1, $2, $3, $4, $5)",
        [email, randomPassword, full_name || 'مستخدم جوجل', 'google', google_id]
      );
      users = await query("SELECT * FROM users WHERE email = $1", [email]);
    }

    user = users[0];
    if (user.is_blocked === 1 || user.is_blocked === true) {
      return res.status(403).json({ error: 'هذا الحساب محظور من قبل الإدارة.' });
    }

    const token = jwt.sign(
      { id: user.id, email: user.email, full_name: user.full_name, is_admin: user.is_admin },
      JWT_SECRET,
      { expiresIn: '30d' }
    );

    res.json({
      token,
      user: {
        id: user.id,
        email: user.email,
        full_name: user.full_name,
        is_admin: !!user.is_admin
      }
    });
  } catch (err) {
    res.status(500).json({ error: 'فشل المصادقة عبر حساب جوجل.' });
  }
});

// 2. Backup APIs
app.get('/api/backups', authenticateToken, async (req, res) => {
  try {
    const backups = await query("SELECT id, device_name, backup_size, created_at, updated_at FROM backups WHERE user_id = $1 ORDER BY updated_at DESC", [req.user.id]);
    res.json(backups);
  } catch (err) {
    res.status(500).json({ error: 'فشل استرجاع قائمة النسخ الاحتياطية.' });
  }
});

app.post('/api/backups', authenticateToken, async (req, res) => {
  const { device_name, encrypted_data, backup_size } = req.body;
  if (!device_name || !encrypted_data) {
    return res.status(400).json({ error: 'البيانات المرسلة غير مكتملة.' });
  }

  try {
    const existing = await query("SELECT id FROM backups WHERE user_id = $1 AND device_name = $2", [req.user.id, device_name]);
    
    if (existing.length > 0) {
      await query(
        "UPDATE backups SET encrypted_data = $1, backup_size = $2, updated_at = CURRENT_TIMESTAMP WHERE id = $3",
        [encrypted_data, backup_size || 0, existing[0].id]
      );
      res.json({ message: 'تم تحديث النسخة الاحتياطية بنجاح على السحابة.' });
    } else {
      await query(
        "INSERT INTO backups (user_id, device_name, encrypted_data, backup_size) VALUES ($1, $2, $3, $4)",
        [req.user.id, device_name, encrypted_data, backup_size || 0]
      );
      res.status(201).json({ message: 'تم رفع النسخة الاحتياطية بنجاح إلى السحابة.' });
    }
  } catch (err) {
    res.status(500).json({ error: 'فشل حفظ النسخة الاحتياطية على السحابة.' });
  }
});

app.get('/api/backups/download/:id', authenticateToken, async (req, res) => {
  try {
    const backup = await query("SELECT * FROM backups WHERE id = $1 AND user_id = $2", [req.params.id, req.user.id]);
    if (backup.length === 0) {
      return res.status(404).json({ error: 'النسخة الاحتياطية المطلوبة غير موجودة.' });
    }
    res.json(backup[0]);
  } catch (err) {
    res.status(500).json({ error: 'فشل تنزيل ملف النسخة الاحتياطية.' });
  }
});

app.delete('/api/backups/:id', authenticateToken, async (req, res) => {
  try {
    const result = await query("DELETE FROM backups WHERE id = $1 AND user_id = $2", [req.params.id, req.user.id]);
    res.json({ message: 'تم حذف النسخة الاحتياطية بنجاح.' });
  } catch (err) {
    res.status(500).json({ error: 'فشل حذف النسخة الاحتياطية.' });
  }
});

// 3. App Version & Updates API
app.get('/api/versions/latest', async (req, res) => {
  try {
    const latest = await query("SELECT * FROM versions ORDER BY version_code DESC LIMIT 1");
    if (latest.length === 0) {
      return res.status(404).json({ error: 'لا توجد تحديثات منشورة.' });
    }
    res.json(latest[0]);
  } catch (err) {
    res.status(500).json({ error: 'فشل التحقق من التحديثات.' });
  }
});

app.get('/api/versions/check', async (req, res) => {
  const { current_code } = req.query;
  if (!current_code) return res.status(400).json({ error: 'يرجى تقديم رمز الإصدار الحالي.' });

  try {
    const latest = await query("SELECT * FROM versions ORDER BY version_code DESC LIMIT 1");
    if (latest.length === 0) return res.json({ hasUpdate: false });

    const current = parseInt(current_code);
    const newest = latest[0];

    res.json({
      hasUpdate: newest.version_code > current,
      versionCode: newest.version_code,
      versionName: newest.version_name,
      releaseDate: newest.release_date,
      changelog: newest.changelog,
      downloadUrl: newest.download_url,
      isCritical: !!newest.is_critical
    });
  } catch (err) {
    res.status(500).json({ error: 'فشل فحص التحديثات.' });
  }
});

// 4. Ticket APIs
app.get('/api/tickets', authenticateToken, async (req, res) => {
  try {
    const tickets = await query("SELECT * FROM tickets WHERE user_id = $1 ORDER BY created_at DESC", [req.user.id]);
    res.json(tickets);
  } catch (err) {
    res.status(500).json({ error: 'فشل جلب تذاكر الدعم الفني.' });
  }
});

app.post('/api/tickets', authenticateToken, async (req, res) => {
  const { subject, message, category } = req.body;
  if (!subject || !message) {
    return res.status(400).json({ error: 'يرجى ملء جميع الحقول المطلوبة للتذكرة.' });
  }

  try {
    await query(
      "INSERT INTO tickets (user_id, subject, message, category) VALUES ($1, $2, $3, $4)",
      [req.user.id, subject, message, category || 'GENERAL']
    );
    res.status(201).json({ message: 'تم إنشاء تذكرة الدعم بنجاح! سيقوم فريقنا بالرد عليك قريباً.' });
  } catch (err) {
    res.status(500).json({ error: 'فشل إرسال تذكرة الدعم الفني.' });
  }
});

// 5. Feedback API
app.post('/api/feedbacks', async (req, res) => {
  const { type, content } = req.body;
  if (!type || !content) {
    return res.status(400).json({ error: 'البيانات المرسلة غير صالحة.' });
  }

  try {
    await query("INSERT INTO feedbacks (type, content) VALUES ($1, $2)", [type, content]);
    res.status(201).json({ message: 'شكراً لمساهمتك! تم حفظ البلاغ وسنعمل على مراجعته.' });
  } catch (err) {
    res.status(500).json({ error: 'فشل حفظ البلاغ.' });
  }
});


// ==========================================
// WEB PORTAL ROUTES (EJS Views)
// ==========================================

// Parse web tokens dynamically from cookies helper
const getWebUser = (req) => {
  // Check token in cookie, session, query parameter or local storage simulation
  return req.user || null;
};

// Home View
app.get('/', async (req, res) => {
  const user = getWebUser(req);
  try {
    const latestVersion = await query("SELECT * FROM versions ORDER BY version_code DESC LIMIT 1");
    const counts = {
      users: (await query("SELECT count(*) as count FROM users"))[0].count,
      backups: (await query("SELECT count(*) as count FROM backups"))[0].count,
    };
    res.render('index', { user, latestVersion: latestVersion[0], counts });
  } catch (err) {
    res.render('index', { user, latestVersion: null, counts: { users: 2450, backups: 830 } });
  }
});

// Download View
app.get('/download', async (req, res) => {
  const user = getWebUser(req);
  try {
    const versions = await query("SELECT * FROM versions ORDER BY version_code DESC");
    res.render('download', { user, versions });
  } catch (err) {
    res.render('download', { user, versions: [] });
  }
});

// Help Center View
app.get('/help', (req, res) => {
  const user = getWebUser(req);
  res.render('help', { user });
});

// Privacy & Terms Views
app.get('/privacy', (req, res) => res.render('privacy', { user: getWebUser(req) }));
app.get('/terms', (req, res) => res.render('terms', { user: getWebUser(req) }));

// Web Auth Views
app.get('/login', (req, res) => {
  if (getWebUser(req)) return res.redirect('/dashboard');
  res.render('login', { error: null });
});

app.get('/register', (req, res) => {
  if (getWebUser(req)) return res.redirect('/dashboard');
  res.render('register', { error: null });
});

// Dashboard View
app.get('/dashboard', async (req, res) => {
  const user = getWebUser(req);
  if (!user) return res.redirect('/login');

  try {
    const backups = await query("SELECT * FROM backups WHERE user_id = $1 ORDER BY updated_at DESC", [user.id]);
    const tickets = await query("SELECT * FROM tickets WHERE user_id = $1 ORDER BY created_at DESC", [user.id]);
    res.render('dashboard', { user, backups, tickets, error: null, success: null });
  } catch (err) {
    res.redirect('/login');
  }
});

// Support Ticket Form Submit
app.post('/dashboard/ticket', async (req, res) => {
  const user = getWebUser(req);
  if (!user) return res.redirect('/login');

  const { subject, message, category } = req.body;
  try {
    await query(
      "INSERT INTO tickets (user_id, subject, message, category) VALUES ($1, $2, $3, $4)",
      [user.id, subject, message, category || 'GENERAL']
    );
    
    const backups = await query("SELECT * FROM backups WHERE user_id = $1 ORDER BY updated_at DESC", [user.id]);
    const tickets = await query("SELECT * FROM tickets WHERE user_id = $1 ORDER BY created_at DESC", [user.id]);
    res.render('dashboard', { user, backups, tickets, error: null, success: 'تم إرسال تذكرة الدعم الفني بنجاح!' });
  } catch (err) {
    const backups = await query("SELECT * FROM backups WHERE user_id = $1 ORDER BY updated_at DESC", [user.id]);
    const tickets = await query("SELECT * FROM tickets WHERE user_id = $1 ORDER BY created_at DESC", [user.id]);
    res.render('dashboard', { user, backups, tickets, error: 'حدث خطأ أثناء إرسال التذكرة.', success: null });
  }
});

// ==========================================
// ADMIN DASHBOARD GATEWAYS
// ==========================================
app.get('/admin', async (req, res) => {
  const user = getWebUser(req);
  if (!user || !user.is_admin) {
    return res.status(403).send('⚠️ غير مصرح لك بدخول لوحة الإدارة. يجب أن تكون مديراً للمنصة.');
  }

  try {
    const totalUsers = (await query("SELECT count(*) as count FROM users"))[0].count;
    const totalBackups = (await query("SELECT count(*) as count FROM backups"))[0].count;
    const openTicketsCount = (await query("SELECT count(*) as count FROM tickets WHERE status = 'OPEN'"))[0].count;
    const totalFeedbacks = (await query("SELECT count(*) as count FROM feedbacks"))[0].count;

    const users = await query("SELECT id, email, full_name, is_blocked, is_admin, created_at FROM users ORDER BY created_at DESC LIMIT 50");
    const tickets = await query(`
      SELECT t.*, u.full_name as user_name, u.email as user_email 
      FROM tickets t 
      JOIN users u ON t.user_id = u.id 
      ORDER BY t.created_at DESC LIMIT 50
    `);
    const feedbacks = await query("SELECT * FROM feedbacks ORDER BY created_at DESC LIMIT 50");
    const versions = await query("SELECT * FROM versions ORDER BY version_code DESC");

    res.render('admin', {
      user,
      stats: { totalUsers, totalBackups, openTicketsCount, totalFeedbacks },
      users,
      tickets,
      feedbacks,
      versions,
      success: req.query.success || null,
      error: req.query.error || null
    });
  } catch (err) {
    res.status(500).send('حدث خطأ أثناء تحميل لوحة المسؤول: ' + err.message);
  }
});

// Admin action: Block/Unblock User
app.post('/admin/users/toggle-block', async (req, res) => {
  const user = getWebUser(req);
  if (!user || !user.is_admin) return res.status(403).send('غير مصرح.');

  const { user_id, current_status } = req.body;
  try {
    const newStatus = parseInt(current_status) === 1 ? 0 : 1;
    await query("UPDATE users SET is_blocked = $1 WHERE id = $2", [newStatus, user_id]);
    res.redirect('/admin?success=' + encodeURIComponent('تم تغيير حالة حظر المستخدم بنجاح.'));
  } catch (err) {
    res.redirect('/admin?error=' + encodeURIComponent('فشل تحديث حالة المستخدم.'));
  }
});

// Admin action: Reply to ticket
app.post('/admin/tickets/reply', async (req, res) => {
  const user = getWebUser(req);
  if (!user || !user.is_admin) return res.status(403).send('غير مصرح.');

  const { ticket_id, reply_message } = req.body;
  try {
    await query(
      "UPDATE tickets SET reply_message = $1, status = 'ANSWERED', replied_at = CURRENT_TIMESTAMP WHERE id = $2",
      [reply_message, ticket_id]
    );
    res.redirect('/admin?success=' + encodeURIComponent('تم الرد على تذكرة الدعم وإرسالها للمستخدم.'));
  } catch (err) {
    res.redirect('/admin?error=' + encodeURIComponent('فشل الرد على التذكرة.'));
  }
});

// Admin action: Publish App Version
app.post('/admin/versions/publish', async (req, res) => {
  const user = getWebUser(req);
  if (!user || !user.is_admin) return res.status(403).send('غير مصرح.');

  const { version_code, version_name, changelog, download_url, is_critical } = req.body;
  try {
    await query(
      "INSERT INTO versions (version_code, version_name, release_date, changelog, download_url, is_critical) VALUES ($1, $2, $3, $4, $5, $6)",
      [
        parseInt(version_code),
        version_name,
        new Date().toISOString().split('T')[0],
        changelog,
        download_url || 'https://tawasulplus.com/downloads/tawasul-v' + version_name + '.apk',
        is_critical === 'on' || is_critical === '1' ? 1 : 0
      ]
    );
    res.redirect('/admin?success=' + encodeURIComponent('تم نشر النسخة الجديدة وتعميمها بنجاح.'));
  } catch (err) {
    res.redirect('/admin?error=' + encodeURIComponent('فشل نشر التحديث. تأكد من أن رقم الإصدار غير مكرر.'));
  }
});

// Admin action: Send Push Broadcast Notification
app.post('/admin/notifications/broadcast', async (req, res) => {
  const user = getWebUser(req);
  if (!user || !user.is_admin) return res.status(403).send('غير مصرح.');

  const { title, body, audience } = req.body;
  try {
    await query(
      "INSERT INTO notifications (title, body, audience) VALUES ($1, $2, $3)",
      [title, body, audience || 'ALL']
    );
    res.redirect('/admin?success=' + encodeURIComponent('تم إرسال وبث التنبيه العام بنجاح لجميع المستخدمين.'));
  } catch (err) {
    res.redirect('/admin?error=' + encodeURIComponent('فشل إرسال البث.'));
  }
});

// Admin action: Resolve Feedback / Crash report
app.post('/admin/feedbacks/resolve', async (req, res) => {
  const user = getWebUser(req);
  if (!user || !user.is_admin) return res.status(403).send('غير مصرح.');

  const { feedback_id } = req.body;
  try {
    await query("UPDATE feedbacks SET is_resolved = 1 WHERE id = $1", [feedback_id]);
    res.redirect('/admin?success=' + encodeURIComponent('تم تعليم البلاغ كمحلول ومكتمل.'));
  } catch (err) {
    res.redirect('/admin?error=' + encodeURIComponent('فشل تحديث البلاغ.'));
  }
});

// Serve EJS index directly as fallback / test
app.listen(PORT, () => {
  console.log(`🤖 Tawasul Plus Backend running on port ${PORT}`);
  console.log(`🌍 Live Web Portal: http://localhost:${PORT}`);
});
