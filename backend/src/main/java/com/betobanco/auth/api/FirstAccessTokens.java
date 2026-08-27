package com.betobanco.auth.api;

import com.betobanco.users.api.UserAccount;

/**
 * Contrato que o modulo {@code auth} publica para o primeiro acesso.
 *
 * <p>Quem cria um aluno sem senha (hoje, o processamento de pagamento) precisa
 * de um token de definicao de senha para colocar no e-mail. O token e o mesmo
 * mecanismo da recuperacao de senha (D4), so muda o proposito e a validade —
 * e nada disso vaza para fora deste modulo.
 */
public interface FirstAccessTokens {

    /**
     * Cria um token FIRST_ACCESS para o usuario e devolve o valor em claro,
     * que so existe neste retorno: no banco fica apenas o hash. Deve ser
     * chamado dentro da transacao que cria o aluno e enfileira o e-mail, para
     * que o link enviado exista no banco ou nada aconteca.
     */
    String criarPara(UserAccount usuario);
}
