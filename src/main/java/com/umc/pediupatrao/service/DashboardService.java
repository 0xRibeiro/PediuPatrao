package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DashboardService {

    // lista de status possiveis
    public static final List<String> STATUS = List.of(
    "RECEBIDO", "EM_PREPARACAO", "PRONTO", "SAIU_PARA_ENTREGA",
    "RETIRADO", "FINALIZADO", "CANCELADO");

    private final PedidoRepository repository;

    public DashboardService(PedidoRepository repository) {this.repository = repository;}

    public Resumo consultar(LocalDate inicio, LocalDate fim, String status) {
        fim = fim == null ? LocalDate.now() : fim;
        inicio = inicio == null ? fim.minusDays(29) : inicio;

        String filtro = status == null || status.isBlank()
                ? null
                : status.trim().toUpperCase(Locale.ROOT);

        if (inicio.isAfter(fim)
                || ChronoUnit.DAYS.between(inicio, fim) > 365
                || fim.equals(LocalDate.MAX)) {
            throw new IllegalArgumentException(
                    "Informe um período válido de até 366 dias.");
        }

        if (filtro != null && !STATUS.contains(filtro)) {
            throw new IllegalArgumentException("Status inválido.");
        }

        List<Pedido> pedidos = repository.buscarNoPeriodo(
                        inicio.atStartOfDay(),
                        fim.plusDays(1).atStartOfDay())
                .stream()
                .filter(p -> filtro == null || filtro.equals(p.getStatus()))
                .toList();

        Map<String, Long> porStatus = new LinkedHashMap<>();
        STATUS.forEach(s -> porStatus.put(s, 0L));

        Map<LocalDate, BigDecimal> porDia = new TreeMap<>();

        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            porDia.put(dia, BigDecimal.ZERO);
        }

        Map<String, ProdutoVendido> produtos = new HashMap<>();

        BigDecimal faturamento = BigDecimal.ZERO;
        long finalizados = 0;

        for (Pedido pedido : pedidos) {
            String situacao = pedido.getStatus() != null
                    && STATUS.contains(pedido.getStatus())
                    ? pedido.getStatus()
                    : "SEM_STATUS";

            porStatus.merge(situacao, 1L, Long::sum);

            if (!"FINALIZADO".equals(pedido.getStatus())) {
                continue;
            }

            finalizados++;

            BigDecimal valor = BigDecimal.valueOf(pedido.getValorTotal())
                    .setScale(2, RoundingMode.HALF_UP);

            faturamento = faturamento.add(valor);

            porDia.merge(
                    pedido.getDataHoraEntrada().toLocalDate(),
                    valor,
                    BigDecimal::add
            );

            if (pedido.getItens() == null) {
                continue;
            }

            for (ItemPedido item : pedido.getItens()) {
                if (item == null
                        || item.getProdutoId() == null
                        || item.getQuantidade() <= 0) {
                    continue;
                }

                String nome = item.getNomeProduto() == null
                        ? "Produto sem nome"
                        : item.getNomeProduto();

                ProdutoVendido produto = new ProdutoVendido(
                        item.getProdutoId(),
                        nome,
                        item.getQuantidade()
                );

                produtos.merge(
                        item.getProdutoId(),
                        produto,
                        (a, b) -> new ProdutoVendido(
                                a.produtoId(),
                                a.nome(),
                                a.quantidade() + b.quantidade()
                        )
                );
            }
        }

        BigDecimal ticket = finalizados == 0
                ? BigDecimal.ZERO.setScale(2)
                : faturamento.divide(
                BigDecimal.valueOf(finalizados),
                2,
                RoundingMode.HALF_UP
        );

        List<ProdutoVendido> ranking = produtos.values().stream()
                .sorted(
                        Comparator.comparingLong(ProdutoVendido::quantidade)
                                .reversed()
                                .thenComparing(ProdutoVendido::nome)
                                .thenComparing(ProdutoVendido::produtoId)
                )
                .limit(10)
                .toList();

        List<VendaDia> vendas = porDia.entrySet().stream()
                .map(e -> new VendaDia(
                        e.getKey(),
                        e.getValue().setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();

        return new Resumo(
                inicio,
                fim,
                filtro,
                pedidos.size(),
                finalizados,
                faturamento.setScale(2, RoundingMode.HALF_UP),
                ticket,
                vendas,
                porStatus,
                ranking
        );
    }

    public record VendaDia(
            LocalDate data,
            BigDecimal faturamento) {
    }

    public record ProdutoVendido(
            String produtoId,
            String nome,
            long quantidade) {
    }

    public record Resumo(
            LocalDate inicio,
            LocalDate fim,
            String status,
            long totalPedidos,
            long pedidosFinalizados,
            BigDecimal faturamento,
            BigDecimal ticketMedio,
            List<VendaDia> vendasPorDia,
            Map<String, Long> pedidosPorStatus,
            List<ProdutoVendido> produtosMaisVendidos) {
    }
}