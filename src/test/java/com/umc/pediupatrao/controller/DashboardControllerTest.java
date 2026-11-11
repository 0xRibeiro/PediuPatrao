package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.service.DashboardService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, DashboardService.class})
@WithMockUser(roles = "GERENTE")
class DashboardControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PedidoRepository repository;

    @MockitoBean
    private UsuarioDetailsService usuarios;

    @Test
    void calculaIndicadoresGraficoERanking() throws Exception {
        when(repository.buscarNoPeriodo(any(), any()))
                .thenReturn(List.of(
                        pedido(
                                "FINALIZADO",
                                "2026-11-01T10:00:00",
                                90,
                                2
                        ),
                        pedido(
                                "FINALIZADO",
                                "2026-11-02T23:59:59",
                                30,
                                1
                        ),
                        pedido(
                                "CANCELADO",
                                "2026-11-01T12:00:00",
                                500,
                                10
                        ),
                        pedido(
                                "RECEBIDO",
                                "2026-11-01T13:00:00",
                                50,
                                1
                        )
                ));

        mvc.perform(get("/api/dashboard")
                        .param("inicio", "2026-11-01")
                        .param("fim", "2026-11-03"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.totalPedidos").value(4))
                .andExpect(jsonPath("$.pedidosFinalizados").value(2))
                .andExpect(jsonPath("$.faturamento").value(120))
                .andExpect(jsonPath("$.ticketMedio").value(60))
                .andExpect(jsonPath("$.pedidosPorStatus.CANCELADO").value(1))
                .andExpect(jsonPath("$.vendasPorDia.length()").value(3))
                .andExpect(jsonPath("$.vendasPorDia[0].faturamento").value(90))
                .andExpect(jsonPath("$.vendasPorDia[1].faturamento").value(30))
                .andExpect(jsonPath("$.vendasPorDia[2].faturamento").value(0))
                .andExpect(jsonPath("$.produtosMaisVendidos[0].quantidade")
                        .value(3));

        verify(repository).buscarNoPeriodo(
                LocalDateTime.parse("2026-11-01T00:00:00"),
                LocalDateTime.parse("2026-11-04T00:00:00")
        );
    }

    @Test
    void retornaZerosQuandoNaoExistemPedidos() throws Exception {
        when(repository.buscarNoPeriodo(any(), any()))
                .thenReturn(List.of());

        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(0))
                .andExpect(jsonPath("$.faturamento").value(0))
                .andExpect(jsonPath("$.ticketMedio").value(0))
                .andExpect(jsonPath("$.vendasPorDia.length()").value(30))
                .andExpect(jsonPath("$.produtosMaisVendidos").isEmpty());
    }

    @Test
    void aplicaFiltroDeStatusEmTodosOsResultados() throws Exception {
        when(repository.buscarNoPeriodo(any(), any()))
                .thenReturn(List.of(
                        pedido(
                                "FINALIZADO",
                                "2026-11-01T10:00:00",
                                90,
                                2
                        ),
                        pedido(
                                "CANCELADO",
                                "2026-11-01T12:00:00",
                                500,
                                10
                        )
                ));

        mvc.perform(get("/api/dashboard")
                        .param("inicio", "2026-11-01")
                        .param("fim", "2026-11-01")
                        .param("status", "CANCELADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(1))
                .andExpect(jsonPath("$.pedidosFinalizados").value(0))
                .andExpect(jsonPath("$.faturamento").value(0))
                .andExpect(jsonPath("$.produtosMaisVendidos").isEmpty());
    }

    @Test
    void rejeitaFiltrosInvalidosSemConsultarBanco() throws Exception {
        mvc.perform(get("/api/dashboard")
                        .param("inicio", "2026-11-02")
                        .param("fim", "2026-11-01"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/dashboard")
                        .param("inicio", "2024-01-01")
                        .param("fim", "2026-11-01"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/dashboard")
                        .param("status", "INVALIDO"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/dashboard")
                        .param("inicio", "31-11-2026"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(repository);
    }

    @Test
    @WithAnonymousUser
    void exigeLoginNaApi() throws Exception {
        mvc.perform(get("/api/dashboard")
                        .servletPath("/api/dashboard"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(repository);
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void bloqueiaAtendente() throws Exception {
        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(repository);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void permiteAdministrador() throws Exception {
        when(repository.buscarNoPeriodo(any(), any()))
                .thenReturn(List.of());

        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    void trataFalhaDoBanco() throws Exception {
        when(repository.buscarNoPeriodo(any(), any()))
                .thenThrow(
                        new DataAccessResourceFailureException("indisponível")
                );

        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.erro").isNotEmpty());
    }

    private Pedido pedido(
            String status,
            String data,
            double total,
            int quantidade) {

        ItemPedido item = new ItemPedido();
        item.setProdutoId("p1");
        item.setNomeProduto("Pizza de calabresa");
        item.setQuantidade(quantidade);

        Pedido pedido = new Pedido();
        pedido.setStatus(status);
        pedido.setDataHoraEntrada(LocalDateTime.parse(data));
        pedido.setValorTotal(total);
        pedido.setItens(List.of(item));

        return pedido;
    }
}