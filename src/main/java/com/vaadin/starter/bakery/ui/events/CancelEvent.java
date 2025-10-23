package com.vaadin.starter.bakery.ui.events;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

/**
 * Evento genérico de cancelamento, geralmente utilizado para indicar que
 * uma ação foi cancelada em um formulário ou componente de UI.
 *
 * <p>
 * Este evento pode ser utilizado em componentes personalizados para notificar
 * ouvintes de que o usuário decidiu cancelar uma operação (como edição ou criação).
 * </p>
 *
 * <p>
 * Exemplo de uso:
 * <pre>{@code
 * form.addListener(CancelEvent.class, e -> {
 *     // lógica ao cancelar
 * });
 * }</pre>
 * </p>
 *
 * @see ComponentEvent
 */
public class CancelEvent extends ComponentEvent<Component> {

    /**
     * Cria uma nova instância do evento {@code CancelEvent}.
     *
     * @param source      o componente de onde o evento se originou
     * @param fromClient  {@code true} se o evento foi disparado a partir do cliente (navegador), {@code false} se do servidor
     */
    public CancelEvent(Component source, boolean fromClient) {
        super(source, fromClient);
    }
}

