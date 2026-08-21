import React, { useState, useEffect } from 'react';
import { supabase } from '../../api/supabase';
import { Plus, Edit2, Trash2, Search, Filter, FileQuestion, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import './QuizBuilder.css';

const QuizBuilder = () => {
    const [questions, setQuestions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [adding, setAdding] = useState(false);

    // Modal State
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [formData, setFormData] = useState({
        banca: '', concurso: '', cargo: '', ano: new Date().getFullYear(),
        materia: '', assunto: '', dificuldade: 'MEDIO',
        enunciado: '', explicacao: '',
        options: [
            { text: '', is_correct: false },
            { text: '', is_correct: false },
            { text: '', is_correct: false },
            { text: '', is_correct: false },
            { text: '', is_correct: false }
        ]
    });

    // Editing State
    const [editingId, setEditingId] = useState(null);

    useEffect(() => {
        fetchQuestions();
    }, []);

    const fetchQuestions = async () => {
        setLoading(true);
        const { data, error } = await supabase
            .from('questions')
            .select(`
                *,
                question_options (*)
            `)
            .order('created_at', { ascending: false });

        if (!error && data) {
            setQuestions(data);
        }
        setLoading(false);
    };

    const handleOptionChange = (index, value) => {
        const newOptions = [...formData.options];
        newOptions[index].text = value;
        setFormData({ ...formData, options: newOptions });
    };

    const handleCorrectOptionChange = (index) => {
        const newOptions = formData.options.map((opt, i) => ({
            ...opt,
            is_correct: i === index
        }));
        setFormData({ ...formData, options: newOptions });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setAdding(true);

        if (editingId) {
            // 1. Update Question
            const { error: updateError } = await supabase
                .from('questions')
                .update({
                    banca: formData.banca,
                    concurso: formData.concurso,
                    cargo: formData.cargo,
                    ano: formData.ano,
                    materia: formData.materia,
                    assunto: formData.assunto,
                    dificuldade: formData.dificuldade,
                    enunciado: formData.enunciado,
                    explicacao: formData.explicacao
                })
                .eq('id', editingId);

            if (updateError) {
                toast.error('Erro ao editar questão: ' + updateError.message);
                setAdding(false);
                return;
            }

            // 2. Delete old Options & Insert New Options
            await supabase.from('question_options').delete().eq('question_id', editingId);

            const optionsToInsert = formData.options
                .filter(opt => opt.text.trim() !== '')
                .map(opt => ({
                    question_id: editingId,
                    text: opt.text,
                    is_correct: opt.is_correct
                }));

            const { error: oError } = await supabase.from('question_options').insert(optionsToInsert);

            if (oError) {
                toast.error('Erro ao atualizar opções: ' + oError.message);
            } else {
                toast.success('Questão atualizada com sucesso!');
                setEditingId(null);
                setIsModalOpen(false);
                resetForm();
                fetchQuestions();
            }
        } else {
            // 1. Insert Question
            const { data: qData, error: qError } = await supabase
                .from('questions')
                .insert([{
                    banca: formData.banca,
                    concurso: formData.concurso,
                    cargo: formData.cargo,
                    ano: formData.ano,
                    materia: formData.materia,
                    assunto: formData.assunto,
                    dificuldade: formData.dificuldade,
                    enunciado: formData.enunciado,
                    explicacao: formData.explicacao,
                    status: 'PUBLICADA'
                }])
                .select();

            if (qError) {
                toast.error('Erro ao salvar questão: ' + qError.message);
                setAdding(false);
                return;
            }

            const questionId = qData[0].id;

            // 2. Insert Options
            const optionsToInsert = formData.options
                .filter(opt => opt.text.trim() !== '')
                .map(opt => ({
                    question_id: questionId,
                    text: opt.text,
                    is_correct: opt.is_correct
                }));

            const { error: oError } = await supabase
                .from('question_options')
                .insert(optionsToInsert);

            if (oError) {
                toast.error('Erro ao salvar opções: ' + oError.message);
            } else {
                toast.success('Questão cadastrada com sucesso!');
                setIsModalOpen(false);
                resetForm();
                fetchQuestions();
            }
        }
        setAdding(false);
    };

    const resetForm = () => {
        setFormData({
            banca: '', concurso: '', cargo: '', ano: new Date().getFullYear(),
            materia: '', assunto: '', dificuldade: 'MEDIO',
            enunciado: '', explicacao: '',
            options: [
                { text: '', is_correct: false }, { text: '', is_correct: false },
                { text: '', is_correct: false }, { text: '', is_correct: false }, { text: '', is_correct: false }
            ]
        });
    };

    const handleEdit = (q) => {
        setEditingId(q.id);
        setFormData({
            banca: q.banca, concurso: q.concurso, cargo: q.cargo, ano: q.ano,
            materia: q.materia, assunto: q.assunto, dificuldade: q.dificuldade,
            enunciado: q.enunciado, explicacao: q.explicacao,
            options: q.question_options
        });
        setIsModalOpen(true);
    };

    const handleDelete = async (id) => {
        if (confirm('Tem certeza que deseja deletar esta questão permanentemente?')) {
            const { error } = await supabase.from('questions').delete().eq('id', id);
            if (error) {
                toast.error('Erro ao deletar: ' + error.message);
            } else {
                toast.success('Questão removida.');
                fetchQuestions();
            }
        }
    };

    return (
        <div className="quiz-builder">
            <div className="qb-header">
                <div>
                    <h3>Banco de Questões</h3>
                    <p>Gerencie todo o acervo de questões do sistema.</p>
                </div>
                <button className="btn-primary" onClick={() => { setEditingId(null); resetForm(); setIsModalOpen(true); }}>
                    <Plus size={18} /> Nova Questão
                </button>
            </div>

            <div className="qb-filters">
                <div className="search-box">
                    <Search size={18} />
                    <input type="text" placeholder="Buscar por enunciado, banco ou matéria..." />
                </div>
                <button className="btn-secondary"><Filter size={18} /> Filtros</button>
            </div>

            {loading ? (
                <div className="qb-loading">Carregando banco de questões...</div>
            ) : questions.length === 0 ? (
                <div className="qb-empty">
                    <FileQuestion size={48} />
                    <p>Nenhuma questão cadastrada ainda.</p>
                </div>
            ) : (
                <div className="table-container">
                    <table className="qb-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Matéria / Assunto</th>
                                <th>Banca / Ano</th>
                                <th>Dificuldade</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {questions.map((q) => (
                                <tr key={q.id}>
                                    <td className="t-muted">...{q.id.slice(-6)}</td>
                                    <td>
                                        <strong>{q.materia}</strong>
                                        <br />
                                        <span className="t-small t-muted">{q.assunto || 'Geral'}</span>
                                    </td>
                                    <td>{q.banca} - {q.ano}</td>
                                    <td><span className={`badge badge-${q.dificuldade.toLowerCase()}`}>{q.dificuldade}</span></td>
                                    <td className="actions-cell">
                                        <button className="action-btn edit" onClick={() => handleEdit(q)}><Edit2 size={16} /></button>
                                        <button className="action-btn delete" onClick={() => handleDelete(q.id)}><Trash2 size={16} /></button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {isModalOpen && (
                <div className="qb-modal-overlay">
                    <div className="qb-modal">
                        <div className="modal-header">
                            <h2>{editingId ? 'Editar Questão' : 'Cadastrar Nova Questão'}</h2>
                            <button onClick={() => setIsModalOpen(false)} className="close-btn">×</button>
                        </div>
                        <form onSubmit={handleSubmit} className="modal-body">
                            <div className="form-row">
                                <div className="form-group">
                                    <label>Matéria *</label>
                                    <input required type="text" value={formData.materia} onChange={e => setFormData({ ...formData, materia: e.target.value })} />
                                </div>
                                <div className="form-group">
                                    <label>Assunto</label>
                                    <input type="text" value={formData.assunto} onChange={e => setFormData({ ...formData, assunto: e.target.value })} />
                                </div>
                            </div>

                            <div className="form-row">
                                <div className="form-group">
                                    <label>Banca Examinadora</label>
                                    <input type="text" value={formData.banca} onChange={e => setFormData({ ...formData, banca: e.target.value })} />
                                </div>
                                <div className="form-group">
                                    <label>Ano</label>
                                    <input type="number" value={formData.ano} onChange={e => setFormData({ ...formData, ano: e.target.value })} />
                                </div>
                                <div className="form-group">
                                    <label>Dificuldade</label>
                                    <select value={formData.dificuldade} onChange={e => setFormData({ ...formData, dificuldade: e.target.value })}>
                                        <option value="FACIL">Fácil</option>
                                        <option value="MEDIO">Médio</option>
                                        <option value="DIFICIL">Difícil</option>
                                        <option value="MUITO_DIFICIL">Muito Difícil</option>
                                    </select>
                                </div>
                            </div>

                            <div className="form-group">
                                <label>Enunciado da Questão *</label>
                                <textarea required rows="4" value={formData.enunciado} onChange={e => setFormData({ ...formData, enunciado: e.target.value })}></textarea>
                            </div>

                            <div className="options-section">
                                <label>Alternativas (Marque a correta)</label>
                                {formData.options.map((opt, idx) => (
                                    <div key={idx} className={`option-row ${opt.is_correct ? 'is-correct' : ''}`}>
                                        <input type="radio" name="correct_option" checked={opt.is_correct} onChange={() => handleCorrectOptionChange(idx)} required />
                                        <span className="opt-letter">{String.fromCharCode(65 + idx)})</span>
                                        <input type="text" value={opt.text} onChange={e => handleOptionChange(idx, e.target.value)} required={idx < 2} />
                                    </div>
                                ))}
                            </div>

                            <div className="form-group">
                                <label>Explicação do Professor</label>
                                <textarea rows="3" value={formData.explicacao} onChange={e => setFormData({ ...formData, explicacao: e.target.value })}></textarea>
                            </div>

                            <div className="modal-footer">
                                <button type="button" className="btn-secondary" onClick={() => setIsModalOpen(false)}>Cancelar</button>
                                <button type="submit" className="btn-primary" disabled={adding}>
                                    {adding ? <Loader2 className="spin" size={20} /> : (editingId ? "Atualizar Questão" : "Salvar Questão")}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default QuizBuilder;
