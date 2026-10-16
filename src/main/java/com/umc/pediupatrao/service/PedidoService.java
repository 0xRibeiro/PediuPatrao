
package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    public Pedido criarPedido(Pedido pedido) {

        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "O pedido precisa ter itens.");
        }

        double total = 0;
        int quantidadeTotal = 0;

        // busca os preços dos produtoos no banco
        for (ItemPedido item : pedido.getItens()) {

            if (item == null || item.getProdutoId() == null
                    || item.getProdutoId().isBlank()
                    || item.getQuantidade() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Item inválido.");
            }

            Produto produto = produtoRepository.findById(item.getProdutoId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Produto não encontrado."));

            if (Boolean.FALSE.equals(produto.getAtivo())
                    || produto.getPreco() == null
                    || !Double.isFinite(produto.getPreco())
                    || produto.getPreco() < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Produto indisponível.");
            }

            item.setNomeProduto(produto.getNome());
            item.setPrecoUnitario(produto.getPreco());

            total += produto.getPreco() * item.getQuantidade();
            quantidadeTotal += item.getQuantidade();
        }

        if (!Double.isFinite(total) || quantidadeTotal <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Total inválido.");
        }

        // Os valores de entrada são definidos no servidor
        pedido.setId(null);
        pedido.setQuantidade(quantidadeTotal);
        pedido.setPercentualDesconto(0);
        pedido.setValorTotal(Math.round(total * 100.0) / 100.0);
        pedido.setStatus("RECEBIDO");

        pedido.setResponsavelEntrada(
                SecurityContextHolder.getContext()
                        .getAuthentication().getName());

        pedido.setDataHoraEntrada(LocalDateTime.now());
        pedido.setResponsavelSaida(null);
        pedido.setDataHoraSaida(null);
        pedido.setJustificativaCancelamento(null);

        return pedidoRepository.save(pedido);
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }


    public Pedido atualizarStatus(String id, String status) {

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pedido não encontrado."));

        String atual = pedido.getStatus() == null ? "" : pedido.getStatus();
        String novo = status == null ? "" : status.trim().toUpperCase();

        // confere se o pedido pode passar para o outro status
        boolean permitido = switch (atual) {
            case "RECEBIDO" -> novo.equals("EM_PREPARACAO");
            case "EM_PREPARACAO" -> novo.equals("PRONTO");
            case "PRONTO" -> novo.equals("SAIU_PARA_ENTREGA")
                    || novo.equals("RETIRADO");
            case "SAIU_PARA_ENTREGA", "RETIRADO" ->
                    novo.equals("FINALIZADO");
            default -> false;
        };

        if (!permitido) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Mudança de status inválida.");
        }

        // salva quem realizou a saida e quando
        if (novo.equals("SAIU_PARA_ENTREGA") || novo.equals("RETIRADO")) {
            pedido.setDataHoraSaida(LocalDateTime.now());
            pedido.setResponsavelSaida(
                    SecurityContextHolder.getContext()
                            .getAuthentication().getName());
        }

        pedido.setStatus(novo);
        return pedidoRepository.save(pedido);
    }



    // confere se o pedido ainda pode ser alterado
    private boolean podeAlterar(Pedido pedido) {
        String status = pedido.getStatus();

        return "RECEBIDO".equals(status)
                || "EM_PREPARACAO".equals(status)
                || "PRONTO".equals(status);
    }

    // aplica os desconto de ate 20%
    public Pedido aplicarDesconto(String id, double percentual) {

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pedido não encontrado."));

        if (!podeAlterar(pedido)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Pedido não permite desconto.");
        }

        if (!Double.isFinite(percentual) || percentual <= 0 || percentual > 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Desconto deve ser entre 0 e 20%.");
        }

        // recalcula com os precos registrados nos itens
        double total = 0;

        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Pedido sem itens.");
        }

        for (ItemPedido item : pedido.getItens()) {
            total += item.getPrecoUnitario() * item.getQuantidade();
        }

        pedido.setPercentualDesconto(percentual);
        pedido.setValorTotal(
                Math.round(total * (1 - percentual / 100) * 100.0) / 100.0
        );

        return pedidoRepository.save(pedido);
    }

    // Cancela o pedido com uma justificativa
    public Pedido cancelarPedido(String id, String justificativa) {

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pedido não encontrado."));

        if (!podeAlterar(pedido)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Pedido não pode ser cancelado.");
        }

        if (justificativa == null || justificativa.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Informe a justificativa.");
        }

        pedido.setStatus("CANCELADO");
        pedido.setJustificativaCancelamento(justificativa.trim());

        return pedidoRepository.save(pedido);
    }


}
