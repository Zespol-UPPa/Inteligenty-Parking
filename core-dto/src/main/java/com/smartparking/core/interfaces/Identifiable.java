package com.smartparking.core.interfaces;

/**
 * Interfejs bazowy dla obiektów posiadających identyfikator.
 * Umożliwia generyczne operacje na encjach i DTO.
 */
public interface Identifiable {

    /**
     * Zwraca unikalny identyfikator obiektu.
     * @return identyfikator (np. ID rekordu, null jeśli obiekt jeszcze nie został zapisany)
     */
    Long getId();
}
