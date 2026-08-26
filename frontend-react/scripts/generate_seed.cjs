const fs = require('fs');
const path = require('path');

const simuladoJsPath = path.join(__dirname, 'public', 'js', 'simulado.js');
const lines = fs.readFileSync(simuladoJsPath, 'utf8').split('\n');

let startIdx = lines.findIndex(l => l.includes('const quizBank = {'));
let endIdx = lines.findIndex((l, i) => i > startIdx && l.startsWith('};'));

let code = lines.slice(startIdx, endIdx + 1).join('\n');
code = code.replace('const quizBank =', 'module.exports =');

const tempPath = path.join(__dirname, 'temp_qb.cjs');
fs.writeFileSync(tempPath, code);

const quizBank = require('./temp_qb.cjs');

let allQuestions = [];
if (quizBank.basico) allQuestions.push(...quizBank.basico.map(q => ({ ...q, difficulty: 'FACIL' })));
if (quizBank.intermediario) allQuestions.push(...quizBank.intermediario.map(q => ({ ...q, difficulty: 'MEDIO' })));
if (quizBank.avancado) allQuestions.push(...quizBank.avancado.map(q => ({ ...q, difficulty: 'DIFICIL' })));

let sql = `-- ==========================================\n`;
sql += `-- APROVAÇÃO PASSO A PASSO - MIGRATION SEEDER\n`;
sql += `-- ==========================================\n\n`;

allQuestions.forEach((q) => {
    const qid = `gen_random_uuid()`;
    let materia = 'Conhecimentos Bancários';

    sql += `DO $$\n`;
    sql += `DECLARE q_id UUID := ${qid};\n`;
    sql += `BEGIN\n`;

    let enunciadoText = (q.prompt || q.text || '').replace(/'/g, "''");

    sql += `  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) `;
    sql += `VALUES (q_id, 'CESGRANRIO', 2024, '${materia}', '${q.difficulty}', '${enunciadoText}', 'PUBLICADA');\n\n`;

    if (q.options) {
        q.options.forEach((optText, optIdx) => {
            let escapedOpt = optText.replace(/'/g, "''");
            let isCorrect = q.answer === optIdx ? 'true' : 'false';
            sql += `  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, '${escapedOpt}', ${isCorrect});\n`;
        });
    }

    sql += `END $$;\n\n`;
});

fs.writeFileSync(path.join(__dirname, 'supabase', 'migrations', 'v3_seed_questions.sql'), sql);
console.log('SQL Seeder gerado! Total:', allQuestions.length);
fs.unlinkSync(tempPath);
