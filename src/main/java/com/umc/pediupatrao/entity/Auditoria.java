
package com.umc.pediupatrao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "auditorias")
public class Auditoria {

    @Id
    private String id;

    private String usuario;
    private String perfil;
    private LocalDateTime dataHora;

    private String operacao;
    private String entidade;
    private String entidadeId;

    private Map<String, Object> valoresAnteriores;
    private Map<String, Object> valoresNovos;

    private String justificativa;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getOperacao() {
        return operacao;
    }

    public void setOperacao(String operacao) {
        this.operacao = operacao;
    }

    public String getEntidade() {
        return entidade;
    }

    public void setEntidade(String entidade) {
        this.entidade = entidade;
    }

    public String getEntidadeId() {
        return entidadeId;
    }

    public void setEntidadeId(String entidadeId) {
        this.entidadeId = entidadeId;
    }

    public Map<String, Object> getValoresAnteriores() {
        return valoresAnteriores;
    }

    public void setValoresAnteriores(Map<String, Object> valoresAnteriores) {
        this.valoresAnteriores = valoresAnteriores;
    }

    public Map<String, Object> getValoresNovos() {
        return valoresNovos;
    }

    public void setValoresNovos(Map<String, Object> valoresNovos) {
        this.valoresNovos = valoresNovos;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }
}
