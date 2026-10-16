
package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    public Cliente novoCliente(Cliente cliente) {
        return salvar(cliente);
    }

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    // Exclui o cliente e registra que foi apagado
    public void excluir(String id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cliente não encontrado."));

        clienteRepository.deleteById(id);

        auditoriaService.registrar(
                "EXCLUIR_CLIENTE", "Cliente", id,
                dados(cliente), Map.of(), null
        );
    }

    // salva o cliente
    public Cliente salvar(Cliente cliente) {

        Cliente anterior = null;

        if (cliente.getId() != null) {
            anterior = clienteRepository.findById(cliente.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Cliente não encontrado."));
        }

        Cliente salvo = clienteRepository.save(cliente);

        if (anterior == null) {
            auditoriaService.registrar(
                    "CRIAR_CLIENTE", "Cliente", salvo.getId(),
                    Map.of(), dados(salvo), null
            );

        } else {
            // compara os dados antigos com os novos
            Map<String, Object> antigos = dados(anterior);
            Map<String, Object> novos = dados(salvo);

            Map<String, Object> antes = new HashMap<>();
            Map<String, Object> depois = new HashMap<>();

            for (String campo : antigos.keySet()) {
                if (!Objects.equals(antigos.get(campo), novos.get(campo))) {
                    antes.put(campo, antigos.get(campo));
                    depois.put(campo, novos.get(campo));
                }
            }

            // Só registra se alguma informação muda
            if (!depois.isEmpty()) {
                auditoriaService.registrar(
                        "ALTERAR_CLIENTE", "Cliente", salvo.getId(),
                        antes, depois, null
                );
            }
        }

        return salvo;
    }

    // Separa os dados que podem aparecer na audiitoria
    private Map<String, Object> dados(Cliente cliente) {
        Map<String, Object> dados = new HashMap<>();

        dados.put("nome", cliente.getNome());
        dados.put("telefone", cliente.getTelefone());
        dados.put("cep", cliente.getCep());
        dados.put("logradouro", cliente.getLogradouro());
        dados.put("numero", cliente.getNumero());
        dados.put("complemento", cliente.getComplemento());
        dados.put("bairro", cliente.getBairro());
        dados.put("cidade", cliente.getCidade());
        dados.put("estado", cliente.getEstado());
        dados.put("endereco", cliente.getEndereco());

        return dados;
    }
}
