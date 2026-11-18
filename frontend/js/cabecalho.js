// Barra do topo, igual em todas as páginas logadas. Monta o HTML dentro de <header id="cabecalho">.
const Cabecalho = {
    // atual: 'inicio', 'meus' ou 'categorias', só para destacar o link da página onde se está
    // O link "Categorias" só aparece para o administrador (o backend barra os outros de qualquer jeito)
    montar(textoBusca = '', atual = 'inicio') {
        document.getElementById('cabecalho').innerHTML = `
            <nav class="navbar navbar-expand-md navbar-mf">
                <div class="container py-1">
                    <a class="navbar-brand fw-bold" href="anuncios.html"><i class="bi bi-bag-heart-fill me-2"></i>Mercado FIPP</a>
                    <ul class="navbar-nav flex-row gap-3 ms-md-3">
                        <li class="nav-item"><a class="nav-link text-white${atual === 'inicio' ? ' fw-bold' : ''}" href="anuncios.html">Início</a></li>
                        <li class="nav-item"><a class="nav-link text-white${atual === 'meus' ? ' fw-bold' : ''}" href="meus-anuncios.html"><i class="bi bi-pencil-square me-1"></i>Meus anúncios</a></li>
                        ${Sessao.ehAdmin() ? `<li class="nav-item"><a class="nav-link text-white${atual === 'categorias' ? ' fw-bold' : ''}" href="categorias.html"><i class="bi bi-tags me-1"></i>Categorias</a></li>` : ''}
                    </ul>
                    <form id="form-busca" class="flex-grow-1 mx-md-4 my-2 my-md-0" role="search">
                        <div class="input-group">
                            <input id="campo-busca" type="search" class="form-control form-control-lg"
                                   placeholder="O que você procura?" aria-label="Buscar anúncios" value="${Util.esc(textoBusca)}">
                            <button class="btn btn-mf-busca" type="submit" aria-label="Buscar"><i class="bi bi-search"></i></button>
                        </div>
                    </form>
                    <div class="d-flex align-items-center gap-2 text-white">
                        <i class="bi bi-person-circle fs-4"></i>
                        <div class="lh-sm text-end">
                            <div class="fw-semibold">${Util.esc(Sessao.nome())}</div>
                            <span class="badge ${Sessao.ehAdmin() ? 'text-bg-danger' : 'text-bg-light'}">${Util.esc(Sessao.nivel() || 'usuário')}</span>
                        </div>
                        <button id="btn-sair" type="button" class="btn btn-outline-light btn-sm ms-2">Sair</button>
                    </div>
                </div>
            </nav>`;

        document.getElementById('btn-sair').addEventListener('click', () => Sessao.sair());
        document.getElementById('form-busca').addEventListener('submit', evento => {
            evento.preventDefault();
            const texto = document.getElementById('campo-busca').value;
            if (typeof window.aoBuscar === 'function') window.aoBuscar(texto);   // a página de anúncios trata a busca
            else location.href = 'anuncios.html?q=' + encodeURIComponent(texto); // as outras levam para ela
        });
    }
};
