
package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    // guarda o que aconteceu e quem fez a alteracão
    public void registrar(String operacao, String entidade, String entidadeId,
                          Map<String, Object> antes, Map<String, Object> depois,
                          String justificativa) {

        Authentication usuario = SecurityContextHolder.getContext().getAuthentication();

        if (usuario == null || !usuario.isAuthenticated()
                || "anonymousUser".equals(usuario.getName())) {
            throw new IllegalStateException("Usuario nao autenticado.");
        }

        String perfil = usuario.getAuthorities().stream()
                .findFirst()
                .map(p -> p.getAuthority().replace("ROLE_", ""))
                .orElse("DESCONHECIDO");

        Auditoria auditoria = new Auditoria();

        auditoria.setUsuario(usuario.getName());
        auditoria.setPerfil(perfil);
        auditoria.setDataHora(LocalDateTime.now());

        auditoria.setOperacao(operacao);
        auditoria.setEntidade(entidade);
        auditoria.setEntidadeId(entidadeId);

        auditoria.setValoresAnteriores(antes);
        auditoria.setValoresNovos(depois);
        auditoria.setJustificativa(justificativa);

        auditoriaRepository.save(auditoria);
    }

    // Lista os registros para a consulta
    public List<Auditoria> listar() {
        return auditoriaRepository.findAll();
    }
}
