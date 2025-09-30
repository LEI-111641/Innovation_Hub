package com.vaadin.starter.bakery.ui.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DebounceSettings;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.DebouncePhase;

/**
 * Componente de interface para pesquisa, representado por uma barra de pesquisa personalizada.
 *
 * Inclui:
 * <ul>
 *   <li>Campo de texto para introdução do termo de pesquisa</li>
 *   <li>Botão de limpar filtro</li>
 *   <li>Botão de ação configurável</li>
 *   <li>Checkbox associado a um estado interno</li>
 * </ul>
 *
 * Permite registar listeners para eventos de filtro e clique em ação.
 */
@Tag("search-bar")
@JsModule("./src/components/search-bar.js")
public class SearchBar extends LitTemplate {

    @Id("field")
    private TextField textField;

    @Id("clear")
    private Button clearButton;

    @Id("action")
    private Button actionButton;

    /**
     * Construtor do componente {@code SearchBar}.
     * Inicializa o campo de texto em modo "eager",
     * adiciona listener para mudanças de valor
     * e associa ação de limpeza ao botão "clear".
     */
    public SearchBar() {
        textField.setValueChangeMode(ValueChangeMode.EAGER);

        ComponentUtil.addListener(textField, SearchValueChanged.class,
                e -> fireEvent(new FilterChanged(this, false)));

        clearButton.addClickListener(e -> {
            textField.clear();
            getElement().setProperty("checkboxChecked", false);
        });

        getElement().addPropertyChangeListener("checkboxChecked",
                e -> fireEvent(new FilterChanged(this, false)));
    }

    /**
     * Obtém o filtro atual introduzido pelo utilizador.
     *
     * @return texto do filtro
     */
    public String getFilter() {
        return textField.getValue();
    }

    /**
     * Verifica se a checkbox interna está marcada.
     *
     * @return {@code true} se marcada, {@code false} caso contrário
     */
    @Synchronize("checkbox-checked-changed")
    public boolean isCheckboxChecked() {
        return getElement().getProperty("checkboxChecked", false);
    }

    /**
     * Define o texto de placeholder no campo de pesquisa.
     *
     * @param placeHolder texto de placeholder
     */
    public void setPlaceHolder(String placeHolder) {
        textField.setPlaceholder(placeHolder);
    }

    /**
     * Define o texto do botão de ação.
     *
     * @param actionText texto do botão
     */
    public void setActionText(String actionText) {
        getElement().setProperty("buttonText", actionText);
    }

    /**
     * Define o texto associado à checkbox.
     *
     * @param checkboxText texto da checkbox
     */
    public void setCheckboxText(String checkboxText) {
        getElement().setProperty("checkboxText", checkboxText);
    }

    /**
     * Adiciona um listener para o evento de alteração do filtro.
     *
     * @param listener callback a executar quando o filtro mudar
     */
    public void addFilterChangeListener(ComponentEventListener<FilterChanged> listener) {
        this.addListener(FilterChanged.class, listener);
    }

    /**
     * Adiciona um listener para o clique no botão de ação.
     *
     * @param listener callback a executar no clique do botão
     */
    public void addActionClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        actionButton.addClickListener(listener);
    }

    /**
     * Obtém a referência ao botão de ação.
     *
     * @return botão de ação
     */
    public Button getActionButton() {
        return actionButton;
    }

    /**
     * Evento personalizado que representa alteração do valor no campo de pesquisa,
     * com debounce de 300ms.
     */
    @DomEvent(value = "value-changed", debounce = @DebounceSettings(timeout = 300, phases = DebouncePhase.TRAILING))
    public static class SearchValueChanged extends ComponentEvent<TextField> {
        public SearchValueChanged(TextField source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Evento personalizado que representa alteração no estado do filtro
     * (campo de pesquisa ou checkbox).
     */
    public static class FilterChanged extends ComponentEvent<SearchBar> {
        public FilterChanged(SearchBar source, boolean fromClient) {
            super(source, fromClient);
        }
    }
}
