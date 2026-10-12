package com.pdv.util;

/** Erro de regra de negócio: a mensagem é exibida diretamente ao operador. */
public class NegocioException extends RuntimeException {
    public NegocioException(String mensagem) { super(mensagem); }
}
