import React, { useState, useEffect } from 'react';
import { supabase } from '../../api/supabase';
import { useAuth } from '../../auth/AuthContext';
import { useNavigate } from 'react-router-dom';
import { Clock, ArrowLeft, CheckCircle, XCircle } from 'lucide-react';
import './SimuladoEngine.css';

const SimuladoEngine = () => {
    const { user } = useAuth();
    const navigate = useNavigate();

    // Core Engine State
    const [loading, setLoading] = useState(true);
    const [questions, setQuestions] = useState([]);
    const [currentIdx, setCurrentIdx] = useState(0);
    const [answers, setAnswers] = useState({}); // { questionId: selectedOptionId }

    // UI State
    const [timeElapsed, setTimeElapsed] = useState(0);
    const [isFinished, setIsFinished] = useState(false);
    const [scoreData, setScoreData] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        fetchRandomQuestions();
    }, []);

    // Timer
    useEffect(() => {
        let interval;
        if (!loading && !isFinished) {
            interval = setInterval(() => {
                setTimeElapsed(prev => prev + 1);
            }, 1000);
        }
        return () => clearInterval(interval);
    }, [loading, isFinished]);

    const fetchRandomQuestions = async () => {
        // Obter Questoes (no futuro, pode aceitar filtro por dificuldade)
        const { data: qData, error } = await supabase
            .from('questions')
            .select(`
                *,
                question_options (*)
            `)
            .eq('status', 'PUBLICADA');

        if (qData) {
            // Embaralhar e pegar 15
            const shuffled = qData.sort(() => 0.5 - Math.random());
            const selected = shuffled.slice(0, 15);
            setQuestions(selected);
        }
        setLoading(false);
    };

    const handleSelectOption = (questionId, optionId) => {
        setAnswers({
            ...answers,
            [questionId]: optionId
        });
    };

    const handleNext = () => {
        if (currentIdx < questions.length - 1) {
            setCurrentIdx(currentIdx + 1);
        }
    };

    const handlePrev = () => {
        if (currentIdx > 0) {
            setCurrentIdx(currentIdx - 1);
        }
    };

    const finishSimulado = async () => {
        if (Object.keys(answers).length < questions.length) {
            if (!confirm('Você ainda tem questões sem responder! Deseja finalizar mesmo assim?')) {
                return;
            }
        }

        setIsSubmitting(true);

        // 1. Calcular Nota
        let correctCount = 0;
        const answerRecords = [];

        questions.forEach(q => {
            const selectedId = answers[q.id];

            // Achar opção Selecionada
            const selectedOpt = q.question_options.find(opt => opt.id === selectedId);
            const isCorrect = selectedOpt ? selectedOpt.is_correct : false;

            if (isCorrect) correctCount++;

            if (selectedId) {
                answerRecords.push({
                    question_id: q.id,
                    selected_option_id: selectedId,
                    is_correct: isCorrect
                });
            }
        });

        const finalScore = Math.round((correctCount / questions.length) * 100);
        setScoreData({ score: finalScore, correct: correctCount, total: questions.length });

        // 2. Salvar na Tabela attempts
        const { data: attemptData, error: attemptError } = await supabase
            .from('attempts')
            .insert([{
                student_id: user.id,
                level: 'GERAL',
                score: finalScore,
                time_spent: timeElapsed
            }])
            .select();

        if (attemptData && answerRecords.length > 0) {
            // 3. Salvar as alternativas marcadas na tabela attempt_answers
            const attemptId = attemptData[0].id;
            const fullAnswerRecords = answerRecords.map(r => ({
                ...r,
                attempt_id: attemptId
            }));

            await supabase
                .from('attempt_answers')
                .insert(fullAnswerRecords);
        }

        setIsSubmitting(false);
        setIsFinished(true);
    };

    const formatTime = (seconds) => {
        const m = Math.floor(seconds / 60).toString().padStart(2, '0');
        const s = (seconds % 60).toString().padStart(2, '0');
        return `${m}:${s}`;
    };

    if (loading) return <div className="simulado-loading"><div className="loader"></div> Carregando Banco Inteligente...</div>;
    if (questions.length === 0) return <div className="simulado-loading">Nenhuma questão disponível no banco de dados!</div>;

    if (isFinished) {
        return (
            <div className="simulado-frame results-mode">
                <div className="r-container">
                    <img src="/logo-beto-banco.jpg" alt="Beto Banco" style={{ height: '40px', borderRadius: '8px' }} />
                    <div className="r-card">
                        <h2>Simulado Finalizado!</h2>

                        <div className="r-score-circle">
                            <span className="score-val">{scoreData?.score}%</span>
                            <span className="score-lbl">Aproveitamento</span>
                        </div>

                        <div className="r-stats">
                            <div className="rst">
                                <strong>Corretas:</strong> {scoreData?.correct} de {scoreData?.total}
                            </div>
                            <div className="rst">
                                <strong>Tempo Gasto:</strong> {formatTime(timeElapsed)}
                            </div>
                        </div>

                        <button className="btn-primary" onClick={() => navigate('/dashboard')}>
                            Voltar para o Dashboard
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    const currentQ = questions[currentIdx];
    const isLast = currentIdx === questions.length - 1;

    return (
        <div className="simulado-frame">
            <header className="qb-header-top">
                <button className="btn-back" onClick={() => navigate('/dashboard')}>
                    <ArrowLeft size={20} /> Sair
                </button>
                <div className="timer-box">
                    <Clock size={18} /> {formatTime(timeElapsed)}
                </div>
                <button className="btn-finish-early" onClick={finishSimulado}>
                    Finalizar
                </button>
            </header>

            <main className="q-main">
                <div className="q-number-box">
                    Questão {currentIdx + 1} <span>/ {questions.length}</span>
                    <div className="q-badge">{currentQ.materia}</div>
                </div>

                <div className="q-enunciado">
                    <p>{currentQ.enunciado}</p>
                </div>

                <div className="q-options">
                    {currentQ.question_options.sort((a, b) => a.id > b.id ? 1 : -1).map((opt, i) => {
                        const isSelected = answers[currentQ.id] === opt.id;
                        return (
                            <button
                                key={opt.id}
                                className={`q-opt-btn ${isSelected ? 'selected' : ''}`}
                                onClick={() => handleSelectOption(currentQ.id, opt.id)}
                            >
                                <span className="opt-letter">{String.fromCharCode(65 + i)}</span>
                                <span className="opt-text">{opt.text}</span>
                                {isSelected && <CheckCircle size={18} className="opt-check" />}
                            </button>
                        );
                    })}
                </div>
            </main>

            <footer className="q-footer">
                <div className="q-progress">
                    <div className="progress-bar" style={{ width: `${((currentIdx + 1) / questions.length) * 100}%` }}></div>
                </div>

                <div className="q-actions">
                    <button className="btn-nav" onClick={handlePrev} disabled={currentIdx === 0}>Anterior</button>
                    {!isLast ? (
                        <button className="btn-nav primary" onClick={handleNext}>Próxima</button>
                    ) : (
                        <button className="btn-nav finish" onClick={finishSimulado} disabled={isSubmitting}>
                            {isSubmitting ? 'Salvando...' : 'Entregar Simulado'}
                        </button>
                    )}
                </div>
            </footer>
        </div>
    );
};

export default SimuladoEngine;
