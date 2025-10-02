package com.vaadin.starter.bakery.backend.service;
//test2
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.vaadin.starter.bakery.backend.data.DashboardData;
import com.vaadin.starter.bakery.backend.data.DeliveryStats;
import com.vaadin.starter.bakery.backend.data.OrderState;
import com.vaadin.starter.bakery.backend.data.entity.Order;
import com.vaadin.starter.bakery.backend.data.entity.OrderSummary;
import com.vaadin.starter.bakery.backend.data.entity.Product;
import com.vaadin.starter.bakery.backend.data.entity.User;
import com.vaadin.starter.bakery.backend.repositories.OrderRepository;

/**
 * Serviço responsável pela gestão de {@link Order} no sistema.
 * Fornece operações de criação, atualização, pesquisa e estatísticas
 * relacionadas com encomendas. Integra com o repositório JPA {@link OrderRepository}.
 */
@Service
public class OrderService implements CrudService<Order> {

    private final OrderRepository orderRepository;

    /**
     * Construtor do serviço de encomendas.
     *
     * @param orderRepository repositório JPA para acesso a dados de encomendas
     */
    @Autowired
    public OrderService(OrderRepository orderRepository) {
        super();
        this.orderRepository = orderRepository;
    }

    private static final Set<OrderState> notAvailableStates = Collections.unmodifiableSet(
            EnumSet.complementOf(EnumSet.of(OrderState.DELIVERED, OrderState.READY, OrderState.CANCELLED)));

    /**
     * Guarda ou cria uma nova encomenda com base no utilizador e num preenchimento externo.
     *
     * @param currentUser utilizador que cria ou atualiza a encomenda
     * @param id          identificador da encomenda (pode ser {@code null} para criar nova)
     * @param orderFiller função de preenchimento da encomenda
     * @return encomenda persistida
     */
    @Transactional(rollbackOn = Exception.class)
    public Order saveOrder(User currentUser, Long id, BiConsumer<User, Order> orderFiller) {
        Order order;
        if (id == null) {
            order = new Order(currentUser);
        } else {
            order = load(id);
        }
        orderFiller.accept(currentUser, order);
        return orderRepository.save(order);
    }

    /**
     * Guarda uma encomenda já existente.
     *
     * @param order encomenda a guardar
     * @return encomenda persistida
     */
    @Transactional(rollbackOn = Exception.class)
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    /**
     * Adiciona um comentário ao histórico da encomenda.
     *
     * @param currentUser utilizador que comenta
     * @param order       encomenda alvo
     * @param comment     comentário a adicionar
     * @return encomenda atualizada
     */
    @Transactional(rollbackOn = Exception.class)
    public Order addComment(User currentUser, Order order, String comment) {
        order.addHistoryItem(currentUser, comment);
        return orderRepository.save(order);
    }

    /**
     * Pesquisa encomendas por nome do cliente e/ou data de entrega após determinada data.
     *
     * @param optionalFilter     filtro pelo nome do cliente
     * @param optionalFilterDate filtro pela data mínima de entrega
     * @param pageable           paginação
     * @return página de encomendas encontradas
     */
    public Page<Order> findAnyMatchingAfterDueDate(Optional<String> optionalFilter,
                                                   Optional<LocalDate> optionalFilterDate, Pageable pageable) {
        if (optionalFilter.isPresent() && !optionalFilter.get().isEmpty()) {
            if (optionalFilterDate.isPresent()) {
                return orderRepository.findByCustomerFullNameContainingIgnoreCaseAndDueDateAfter(
                        optionalFilter.get(), optionalFilterDate.get(), pageable);
            } else {
                return orderRepository.findByCustomerFullNameContainingIgnoreCase(optionalFilter.get(), pageable);
            }
        } else {
            if (optionalFilterDate.isPresent()) {
                return orderRepository.findByDueDateAfter(optionalFilterDate.get(), pageable);
            } else {
                return orderRepository.findAll(pageable);
            }
        }
    }

    /**
     * Obtém resumo de encomendas a partir de hoje.
     *
     * @return lista de encomendas futuras
     */
    @Transactional
    public List<OrderSummary> findAnyMatchingStartingToday() {
        return orderRepository.findByDueDateGreaterThanEqual(LocalDate.now());
    }

    /**
     * Conta encomendas filtradas por nome do cliente e/ou data de entrega.
     *
     * @param optionalFilter     filtro pelo nome do cliente
     * @param optionalFilterDate filtro pela data mínima de entrega
     * @return número de encomendas encontradas
     */
    public long countAnyMatchingAfterDueDate(Optional<String> optionalFilter, Optional<LocalDate> optionalFilterDate) {
        if (optionalFilter.isPresent() && optionalFilterDate.isPresent()) {
            return orderRepository.countByCustomerFullNameContainingIgnoreCaseAndDueDateAfter(optionalFilter.get(),
                    optionalFilterDate.get());
        } else if (optionalFilter.isPresent()) {
            return orderRepository.countByCustomerFullNameContainingIgnoreCase(optionalFilter.get());
        } else if (optionalFilterDate.isPresent()) {
            return orderRepository.countByDueDateAfter(optionalFilterDate.get());
        } else {
            return orderRepository.count();
        }
    }

    /**
     * Obtém estatísticas de entregas para o dashboard.
     *
     * @return estatísticas de entregas
     */
    private DeliveryStats getDeliveryStats() {
        DeliveryStats stats = new DeliveryStats();
        LocalDate today = LocalDate.now();
        stats.setDueToday((int) orderRepository.countByDueDate(today));
        stats.setDueTomorrow((int) orderRepository.countByDueDate(today.plusDays(1)));
        stats.setDeliveredToday((int) orderRepository.countByDueDateAndStateIn(today,
                Collections.singleton(OrderState.DELIVERED)));

        stats.setNotAvailableToday((int) orderRepository.countByDueDateAndStateIn(today, notAvailableStates));
        stats.setNewOrders((int) orderRepository.countByState(OrderState.NEW));

        return stats;
    }

    /**
     * Obtém dados do dashboard (estatísticas, entregas e vendas).
     *
     * @param month mês de referência
     * @param year  ano de referência
     * @return dados agregados do dashboard
     */
    public DashboardData getDashboardData(int month, int year) {
        DashboardData data = new DashboardData();
        data.setDeliveryStats(getDeliveryStats());
        data.setDeliveriesThisMonth(getDeliveriesPerDay(month, year));
        data.setDeliveriesThisYear(getDeliveriesPerMonth(year));

        Number[][] salesPerMonth = new Number[3][12];
        data.setSalesPerMonth(salesPerMonth);
        List<Object[]> sales = orderRepository.sumPerMonthLastThreeYears(OrderState.DELIVERED, year);

        for (Object[] salesData : sales) {
            // year, month, deliveries
            int y = year - (int) salesData[0];
            int m = (int) salesData[1] - 1;
            if (y == 0 && m == month - 1) {
                // ignora mês atual (dados incompletos)
                continue;
            }
            long count = (long) salesData[2];
            salesPerMonth[y][m] = count;
        }

        LinkedHashMap<Product, Integer> productDeliveries = new LinkedHashMap<>();
        data.setProductDeliveries(productDeliveries);
        for (Object[] result : orderRepository.countPerProduct(OrderState.DELIVERED, year, month)) {
            int sum = ((Long) result[0]).intValue();
            Product p = (Product) result[1];
            productDeliveries.put(p, sum);
        }

        return data;
    }

    private List<Number> getDeliveriesPerDay(int month, int year) {
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        return flattenAndReplaceMissingWithNull(daysInMonth,
                orderRepository.countPerDay(OrderState.DELIVERED, year, month));
    }

    private List<Number> getDeliveriesPerMonth(int year) {
        return flattenAndReplaceMissingWithNull(12, orderRepository.countPerMonth(OrderState.DELIVERED, year));
    }

    private List<Number> flattenAndReplaceMissingWithNull(int length, List<Object[]> list) {
        List<Number> counts = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            counts.add(null);
        }

        for (Object[] result : list) {
            counts.set((Integer) result[0] - 1, (Number) result[1]);
        }
        return counts;
    }

    /**
     * Obtém o repositório associado a {@link Order}.
     *
     * @return repositório JPA de encomendas
     */
    @Override
    public JpaRepository<Order, Long> getRepository() {
        return orderRepository;
    }

    /**
     * Cria uma nova encomenda para o utilizador atual, com data e hora predefinidas.
     *
     * @param currentUser utilizador associado à encomenda
     * @return nova encomenda inicializada
     */
    @Override
    @Transactional
    public Order createNew(User currentUser) {
        Order order = new Order(currentUser);
        order.setDueTime(LocalTime.of(16, 0));
        order.setDueDate(LocalDate.now());
        return order;
    }
    /**
     * Verifica se uma encomenda está atrasada (data de entrega anterior a hoje).
     *
     * @param order encomenda a verificar
     * @return {@code true} se a encomenda estiver atrasada, caso contrário {@code false}
     */
    public boolean isOrderOverdue(Order order) {
        return order.getDueDate() != null && order.getDueDate().isBefore(LocalDate.now())
                && order.getState() != OrderState.DELIVERED
                && order.getState() != OrderState.CANCELLED;
    }

}
