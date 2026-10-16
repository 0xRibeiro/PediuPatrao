
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
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();
        pedido.setStatus(status);
        return pedidoRepository.save(pedido);
    }
}
