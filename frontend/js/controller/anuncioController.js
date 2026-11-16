// Conversa com apis/adm/anuncio (administrador) e apis/usuario (vendedor/comprador).
//
// CONTRATO ESPERADO DO JSON de um anúncio (nomes iguais às colunas do banco):
//   { anu_id, anu_title, anu_date, anu_desc, anu_price, cat_id, cat_name, usr_id, usr_name,
//     fotos: [ { fot_id, fot_file } ], perguntas: [ { per_id, per_text, per_resp } ] }
// (é exatamente o que o AdmAnuncioRestController do backend devolve)
// Se o backend usar outros nomes, o ÚNICO lugar para ajustar é a função "normalizar" abaixo.

// Devolve o primeiro valor que existir entre as chaves, ou null
function pegar(objeto, ...chaves) {
    for (const chave of chaves) {
        if (objeto && objeto[chave] !== undefined && objeto[chave] !== null) return objeto[chave];
    }
    return null;
}

function normalizar(a) {
    const cat = pegar(a, 'categoria', 'category');
    let catId = pegar(a, 'cat_id');
    let catNome = pegar(a, 'cat_name');
    if (cat && typeof cat === 'object') {
        catId = pegar(cat, 'cat_id', 'id') ?? catId;
        catNome = pegar(cat, 'cat_name', 'nome', 'name') ?? catNome;
    } else if (typeof cat === 'string') {
        catNome = cat;
    }
    if (!catNome && catId !== null) catNome = 'Categoria ' + catId;

    const usr = pegar(a, 'usuario', 'user');
    const usrId = pegar(a, 'usr_id');
    let vendedor = usr && typeof usr === 'object' ? pegar(usr, 'usr_name', 'nome', 'name') : null;
    if (!vendedor) vendedor = pegar(a, 'usr_name');
    if (!vendedor && usrId !== null) vendedor = 'Vendedor #' + usrId;

    const preco = pegar(a, 'anu_price', 'preco', 'price');

    return {
        id: pegar(a, 'anu_id', 'id'),
        titulo: pegar(a, 'anu_title', 'titulo', 'title') ?? '(sem título)',
        data: pegar(a, 'anu_date', 'data', 'date'),
        descricao: pegar(a, 'anu_desc', 'descricao', 'description') ?? '',
        preco: preco === null ? null : Number(preco),
        categoria: { id: catId, nome: catNome },
        vendedor,
        vendedorId: usrId,
        fotos: (pegar(a, 'fotos', 'fotoAnuncios') ?? [])
            .map(f => typeof f === 'string' ? f : pegar(f, 'fot_file', 'arquivo', 'file'))
            .filter(Boolean),
        perguntas: (pegar(a, 'perguntas', 'perguntaAnuncios') ?? []).map(p => ({
            texto: pegar(p, 'per_text', 'texto', 'text') ?? '',
            resposta: pegar(p, 'per_resp', 'resposta', 'answer')
        }))
    };
}

function resumir(dados) {
    const texto = typeof dados === 'string' ? dados : JSON.stringify(dados);
    return texto.length > 80 ? texto.slice(0, 80) + '…' : texto;
}

const AnuncioController = {
    // Administrador usa as rotas adm; vendedor/comprador usa as de usuario
    base() {
        return Sessao.ehAdmin() ? Config.ROTA_ADM_ANUNCIO : Config.ROTA_USUARIO;
    },

    // GET .../get-filter/{filtro} -> lista de anúncios. Texto vazio = todos.
    async filtrar(texto) {
        const filtro = texto.trim() || Config.FILTRO_TODOS;
        const dados = await Http.requisitar(`${this.base()}/get-filter/${encodeURIComponent(filtro)}`);
        if (!Array.isArray(dados)) {
            throw new ErroHttp(0, 'O servidor ainda não devolve a lista de anúncios em JSON. Resposta recebida: "' + resumir(dados) + '"');
        }
        return dados.map(normalizar);
    },

    // GET .../get-id/{id} -> um anúncio
    async buscar(id) {
        const dados = await Http.requisitar(`${this.base()}/get-id/${encodeURIComponent(id)}`);
        if (!dados || typeof dados !== 'object' || Array.isArray(dados)) {
            throw new ErroHttp(0, 'O servidor ainda não devolve o anúncio em JSON. Resposta recebida: "' + resumir(dados) + '"');
        }
        return normalizar(dados);
    },

    // GET apis/usuario/meus -> só os anúncios de quem está logado (o backend lê o dono do token)
    async meus() {
        const dados = await Http.requisitar(`${Config.ROTA_USUARIO}/meus`);
        if (!Array.isArray(dados)) throw new ErroHttp(0, 'O servidor devolveu a lista de anúncios de um jeito inesperado.');
        return dados.map(normalizar);
    },

    // PUT apis/usuario/meus/{id}: edita título, descrição, preço e categoria de um anúncio SEU
    async editar(id, { titulo, descricao, preco, categoriaId }) {
        const dados = await Http.requisitar(`${Config.ROTA_USUARIO}/meus/${encodeURIComponent(id)}`, {
            metodo: 'PUT',
            corpo: { anu_title: titulo, anu_desc: descricao, anu_price: preco, cat_id: categoriaId }
        });
        return normalizar(dados);
    },

    // DELETE apis/adm/anuncio?id=ID (só administrador)
    async excluir(id) {
        await Http.requisitar(`${Config.ROTA_ADM_ANUNCIO}?id=${encodeURIComponent(id)}`, { metodo: 'DELETE' });
    }
};
