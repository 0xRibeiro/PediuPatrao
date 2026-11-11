package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Pedido;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends MongoRepository<Pedido, String> {
    List<Pedido> findByClienteId(String clienteId);

    // query usada pra buscar por dados de um periodo especifico.
    @Query("{ 'dataHoraEntrada': { '$gte': ?0, '$lt': ?1 } }")
    List<Pedido> buscarNoPeriodo(
            LocalDateTime inicio,
            LocalDateTime fimExclusivo);
}