(() => {
    const form = document.getElementById('filtros-dashboard');
    const mensagem = document.getElementById('mensagem-dashboard');
    const resultados = document.getElementById('resultados-dashboard');
    const botao = document.getElementById('atualizar-dashboard');

    const moeda = new Intl.NumberFormat('pt-BR', {
        style: 'currency',
        currency: 'BRL'
    });

    let graficoVendas;
    let graficoStatus;

    function dataLocal(data) {
        return `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}-${String(data.getDate()).padStart(2, '0')}`;
    }

    const hoje = new Date();
    const inicio = new Date(hoje);

    inicio.setDate(inicio.getDate() - 29);

    form.elements.inicio.value = dataLocal(inicio);
    form.elements.fim.value = dataLocal(hoje);

    async function atualizar() {
        if (botao.disabled || !form.reportValidity()) return;

        const parametros = new URLSearchParams(new FormData(form));

        botao.disabled = true;
        resultados.hidden = true;
        mensagem.className = 'text-muted';
        mensagem.textContent = 'Carregando dados...';

        try {
            const resposta = await fetch(
                `${form.dataset.api}?${parametros}`,
                {
                    headers: {
                        Accept: 'application/json'
                    },
                    cache: 'no-store',
                    signal: AbortSignal.timeout(15000)
                }
            );

            if (resposta.status === 401 || resposta.redirected) {
                throw new Error(
                    'Sua sessão expirou. Entre novamente no sistema.'
                );
            }

            if (resposta.status === 403) {
                throw new Error(
                    'Você não tem permissão para acessar o dashboard.'
                );
            }

            const dados = await resposta.json().catch(() => {
                throw new Error(
                    'O servidor retornou uma resposta inválida.'
                );
            });

            if (!resposta.ok) {
                throw new Error(
                    dados.erro || 'Não foi possível consultar os dados.'
                );
            }

            document.getElementById('total-pedidos').textContent =
                dados.totalPedidos;

            document.getElementById('pedidos-finalizados').textContent =
                dados.pedidosFinalizados;

            document.getElementById('faturamento').textContent =
                moeda.format(dados.faturamento);

            document.getElementById('ticket-medio').textContent =
                moeda.format(dados.ticketMedio);

            const ranking = document.getElementById('ranking-produtos');

            ranking.replaceChildren();

            dados.produtosMaisVendidos.forEach((produto, indice) => {
                const linha = ranking.insertRow();

                [indice + 1, produto.nome, produto.quantidade]
                    .forEach(valor => {
                        linha.insertCell().textContent = valor;
                    });
            });

            if (dados.produtosMaisVendidos.length === 0) {
                const celula = ranking.insertRow().insertCell();

                celula.colSpan = 3;
                celula.textContent =
                    'Nenhum produto vendido no período e status selecionados.';
            }

            resultados.hidden = false;

            if (graficoVendas) graficoVendas.destroy();
            if (graficoStatus) graficoStatus.destroy();

            graficoVendas = new Chart(
                document.getElementById('grafico-vendas'),
                {
                    type: 'bar',
                    data: {
                        labels: dados.vendasPorDia.map(dia =>
                            dia.data.split('-').reverse().join('/')
                        ),
                        datasets: [{
                            label: 'Faturamento',
                            backgroundColor: '#0d6efd',
                            data: dados.vendasPorDia.map(dia =>
                                dia.faturamento
                            )
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        legend: {
                            display: false
                        },
                        scales: {
                            yAxes: [{
                                ticks: {
                                    beginAtZero: true,
                                    callback: valor => moeda.format(valor)
                                }
                            }]
                        },
                        tooltips: {
                            callbacks: {
                                label: item => moeda.format(item.yLabel)
                            }
                        }
                    }
                }
            );

            const situacoes = Object.entries(dados.pedidosPorStatus);

            graficoStatus = new Chart(
                document.getElementById('grafico-status'),
                {
                    type: 'doughnut',
                    data: {
                        labels: situacoes.map(([status, quantidade]) =>
                            `${status.replace(/_/g, ' ')}: ${quantidade}`
                        ),
                        datasets: [{
                            data: situacoes.map(([, quantidade]) =>
                                quantidade
                            ),
                            backgroundColor: [
                                '#0d6efd',
                                '#ffc107',
                                '#20c997',
                                '#6f42c1',
                                '#0dcaf0',
                                '#198754',
                                '#dc3545',
                                '#6c757d'
                            ]
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        legend: {
                            position: 'bottom'
                        }
                    }
                }
            );

            mensagem.textContent = dados.totalPedidos === 0
                ? 'Nenhum pedido encontrado para os filtros informados.'
                : `Atualizado em ${new Date().toLocaleString('pt-BR')}.`;

        } catch (erro) {
            resultados.hidden = true;
            mensagem.className = 'alert alert-danger';

            mensagem.textContent = erro.name === 'TimeoutError'
                ? 'A consulta demorou demais. Tente novamente.'
                : erro instanceof TypeError
                    ? 'Não foi possível conectar ao servidor. Tente novamente.'
                    : erro.message;
        } finally {
            botao.disabled = false;
        }
    }

    form.addEventListener('submit', evento => {
        evento.preventDefault();
        atualizar();
    });

    atualizar();
})();