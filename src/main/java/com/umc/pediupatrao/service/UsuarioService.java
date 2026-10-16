package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class UsuarioService {

    private static final Set<String> PERFIS_VALIDOS =
            Set.of("ADMIN", "GERENTE", "ATENDENTE");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Busca um usuário por ID
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    // Busca um usuário por username
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    // Salva um novo usuário (criptografando a senha)
    public void salvarUsuario(Usuario usuario) {
        usuario.setRole(validarRoleDeUsuario(usuario.getRole()));
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword())); // Criptografa a senha
        usuarioRepository.save(usuario);
    }

    // Deleta um usuário pelo ID
    public void deletarUsuario(String id) {
        usuarioRepository.deleteById(id);
    }

    // Lista todos os usuários
    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // Atualiza o usuário usuários
    public void atualizarUsuario(String id, Usuario usuarioAtualizado) {
        String perfil = validarRoleDeUsuario(usuarioAtualizado.getRole());

        usuarioRepository.findById(id).ifPresent(usuarioExistente -> {
            usuarioExistente.setUsername(usuarioAtualizado.getUsername());
            usuarioExistente.setRole(perfil);

            if (usuarioAtualizado.getPassword() != null
                    && !usuarioAtualizado.getPassword().isBlank()) {
                usuarioExistente.setPassword(
                        passwordEncoder.encode(usuarioAtualizado.getPassword())
                );
            }

            usuarioRepository.save(usuarioExistente);
        });
    }

    private String validarRoleDeUsuario(String perfil) {
        if (perfil == null || !PERFIS_VALIDOS.contains(perfil.trim().toUpperCase())) {
            throw new IllegalArgumentException("Perfil inválido.");
        }

        return perfil.trim().toUpperCase();
    }
}
