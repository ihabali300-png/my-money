/* فلوسي 💰 — كود الشيت (Google Apps Script)
   Extensions → Apps Script → امسح اللي موجود والصق ده → Save
   Deploy → New deployment → Web app → Execute as: Me · Who has access: Anyone → Deploy
   انسخ الرابط اللي آخره /exec وحطه في الصفحة ← ⚙️ الإعدادات ← الربط بشيت جوجل */

const TOKEN = 'ihab-money';   // لو غيّرتها هنا غيّرها في الصفحة كمان

const TX = 'المعاملات', BUD = 'الميزانية', SET = 'الإعدادات';
const TX_HEAD = ['ID', 'التاريخ', 'النوع', 'القسم', 'المبلغ', 'العملة', 'ملاحظة', 'اتسجل في', 'cat'];
const CAT_NAMES = { food:'🍔 أكل وشرب', grocery:'🛒 بقالة', transport:'🚕 مواصلات', rent:'🏠 سكن', bills:'📱 فواتير ونت', clothes:'👕 لبس', fun:'🎮 ترفيه', health:'💊 علاج',
  family:'🇪🇬 تحويل لمصر', building:'🏗️ العمارة', wedding:'💍 الخطوبة', gifts:'🎁 هدايا', savings:'🏦 ادخار', other:'📦 حاجات تانية',
  salary:'💼 مرتب', commission:'🏆 عمولة وتارجت', side:'💡 شغل جانبي', otherin:'💵 دخل تاني' };

function doGet() { setup_(); return out_({ ok: true, msg: 'فلوسي شغال ✅' }); }

function doPost(e) {
  let b; try { b = JSON.parse(e.postData.contents); } catch (x) { return out_({ ok: false, error: 'bad json' }); }
  if (b.token !== TOKEN) return out_({ ok: false, error: 'wrong token' });
  const lock = LockService.getScriptLock(); lock.waitLock(20000);
  try {
    setup_();
    switch (b.action) {
      case 'ping': return out_({ ok: true });
      case 'save': if (!findRow_(b.tx.id)) sh_(TX).appendRow(row_(b.tx)); return out_({ ok: true });
      case 'update': { const r = findRow_(b.tx.id); if (r) sh_(TX).getRange(r, 1, 1, TX_HEAD.length).setValues([row_(b.tx)]); else sh_(TX).appendRow(row_(b.tx)); return out_({ ok: true }); }
      case 'delete': { const r = findRow_(b.id); if (r) sh_(TX).deleteRow(r); return out_({ ok: true }); }
      case 'setBudget': {
        const s = sh_(BUD); if (s.getLastRow() > 1) s.getRange(2, 1, s.getLastRow() - 1, 3).clearContent();
        const rows = Object.keys(b.budget || {}).filter(k => +b.budget[k] > 0).map(k => [CAT_NAMES[k] || k, +b.budget[k], k]);
        if (rows.length) s.getRange(2, 1, rows.length, 3).setValues(rows);
        return out_({ ok: true });
      }
      case 'setSetting': {
        const s = sh_(SET), v = s.getDataRange().getValues();
        for (let i = 1; i < v.length; i++) if (v[i][0] === b.key) { s.getRange(i + 1, 2).setValue(b.value); return out_({ ok: true }); }
        s.appendRow([b.key, b.value]); return out_({ ok: true });
      }
      case 'getAll': {
        const tv = sh_(TX).getDataRange().getValues().slice(1).filter(r => r[0]);
        const tx = tv.map(r => ({ id: String(r[0]), date: str_(r[1]), type: r[2] === 'دخل' ? 'in' : 'out', amount: +r[4], cur: r[5] === 'جنيه' ? 'EGP' : 'SAR', note: String(r[6] || ''), created: str_(r[7]), cat: String(r[8] || 'other') }));
        const budget = {}; sh_(BUD).getDataRange().getValues().slice(1).forEach(r => { if (r[2]) budget[r[2]] = +r[1]; });
        const settings = {}; sh_(SET).getDataRange().getValues().slice(1).forEach(r => { if (r[0]) settings[r[0]] = r[1]; });
        return out_({ ok: true, tx, budget, settings });
      }
    }
    return out_({ ok: false, error: 'unknown action' });
  } catch (err) { return out_({ ok: false, error: String(err) }); }
  finally { lock.releaseLock(); }
}

function row_(t) {
  return ["'" + t.id, "'" + t.date, t.type === 'in' ? 'دخل' : 'مصروف', CAT_NAMES[t.cat] || t.cat, +t.amount, t.cur === 'EGP' ? 'جنيه' : 'ريال', "'" + (t.note || ''),
    "'" + Utilities.formatDate(new Date(t.created || Date.now()), 'Asia/Riyadh', 'yyyy-MM-dd HH:mm'), t.cat];
}
function str_(v) { return v instanceof Date ? Utilities.formatDate(v, 'Asia/Riyadh', 'yyyy-MM-dd') : String(v || ''); }
function findRow_(id) { const f = sh_(TX).getRange('A:A').createTextFinder(String(id)).matchEntireCell(true).findNext(); return f ? f.getRow() : 0; }
function sh_(n) { return SpreadsheetApp.getActive().getSheetByName(n); }
function setup_() {
  const ss = SpreadsheetApp.getActive();
  [[TX, TX_HEAD], [BUD, ['القسم', 'الحد الشهري (ريال)', 'cat']], [SET, ['المفتاح', 'القيمة']]].forEach(([n, h]) => {
    let s = ss.getSheetByName(n); if (s) return;
    s = ss.insertSheet(n); s.getRange(1, 1, 1, h.length).setValues([h]).setFontWeight('bold').setBackground('#0c1c12').setFontColor('#a3e635');
    s.setFrozenRows(1); s.setRightToLeft(true);
  });
}
function out_(o) { return ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON); }
