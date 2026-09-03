package br.com.aprovacao.auth;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * A tabela perfil usa ids 1..6 (ver V1__baseline_modelo_de_dados.sql), enquanto o
 * ordinal do enum comeca em 0. Mapear por ORDINAL puro gravaria ALUNO como 0 e
 * violaria a chave estrangeira; este conversor faz a ponte em um lugar so.
 */
@Converter(autoApply = false)
public class ConversorPerfil implements AttributeConverter<Perfil, Short> {

    @Override
    public Short convertToDatabaseColumn(Perfil perfil) {
        return perfil == null ? null : (short) (perfil.ordinal() + 1);
    }

    @Override
    public Perfil convertToEntityAttribute(Short id) {
        if (id == null) {
            return null;
        }
        int indice = id - 1;
        Perfil[] valores = Perfil.values();
        if (indice < 0 || indice >= valores.length) {
            throw new IllegalStateException("perfil_id desconhecido no banco: " + id);
        }
        return valores[indice];
    }
}
