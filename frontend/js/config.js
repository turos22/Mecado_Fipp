// Configuração do front. Se o backend mudar de endereço ou de rota, é só aqui.
const Config = {
    // Endereço do backend (termina com "/")
    API: 'http://localhost:8080/',

    // Rotas (as mesmas da especificação do professor)
    ROTA_LOGIN: 'apis/autenticacao/login',
    ROTA_ADM_ANUNCIO: 'apis/adm/anuncio',   // administrador: get-id/{id}, get-filter/{filtro}, DELETE ?id=
    ROTA_USUARIO: 'apis/usuario',           // vendedor/comprador: get-id/{id}, get-filter/{filtro}
    ROTA_ADM_CATEGORIA: 'apis/adm/categoria', // administrador: get-id/{id}, POST, PUT, DELETE ?id=; get-all: qualquer usuário logado

    // Busca vazia: o front pede este "filtro" ao get-filter. COMBINAR COM QUEM FAZ O BACKEND:
    // o get-filter tem que devolver TODOS os anúncios quando receber este valor.
    FILTRO_TODOS: 'todos',

    // De onde o navegador baixa as imagens (fot_file é só o nome do arquivo). Combinar com o backend.
    URL_FOTOS: 'http://localhost:8080/fotos/'
};
