package com.florez.backend.common.application;

import java.util.function.Supplier;

/**
 * Ejecuta un bloque de trabajo de forma atómica. Permite a los casos de uso delimitar sus
 * transacciones sin depender de Spring (la implementación vive en infraestructura).
 */
public interface TransactionRunner {

    <T> T inTransaction(Supplier<T> work);
}
