package com.vaadin.starter.bakery.backend.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.vaadin.starter.bakery.backend.data.entity.Product;
import com.vaadin.starter.bakery.backend.data.entity.User;
import com.vaadin.starter.bakery.backend.repositories.ProductRepository;

/**
 * Serviço para gerenciamento de {@link Product} que implementa operações CRUD 
 * filtráveis, utilizando um repositório JPA para persistência.
 * 
 * <p>Fornece métodos para buscar produtos, contar produtos com filtros,
 * criar novos produtos e salvar produtos, garantindo validação de unicidade
 * de nome para evitar duplicidades.</p>
 * 
 * <p>Esta classe é anotada como {@link Service}, sendo gerenciada pelo
 * Spring Framework.</p>
 * 
 * @see FilterableCrudService
 * @see ProductRepository
 */
@Service
public class ProductService implements FilterableCrudService<Product> {

	private final ProductRepository productRepository;

	/**
	 * Construtor que injeta o {@link ProductRepository} para operações
	 * de persistência.
	 * 
	 * @param productRepository o repositório de produtos
	 */
	@Autowired
	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	/**
	 * Busca uma página de produtos que correspondam ao filtro opcional fornecido.
	 * Se o filtro estiver presente, busca produtos cujo nome contenha o valor do filtro,
	 * ignorando maiúsculas/minúsculas. Caso contrário, retorna todos os produtos
	 * paginados.
	 * 
	 * @param filter filtro opcional para busca por nome do produto
	 * @param pageable objeto para paginação dos resultados
	 * @return uma página de produtos que correspondem ao filtro
	 */
	@Override
	public Page<Product> findAnyMatching(Optional<String> filter, Pageable pageable) {
		if (filter.isPresent()) {
			String repositoryFilter = "%" + filter.get() + "%";
			return productRepository.findByNameLikeIgnoreCase(repositoryFilter, pageable);
		} else {
			return find(pageable);
		}
	}

	/**
	 * Conta o número total de produtos que correspondam ao filtro opcional.
	 * Se o filtro estiver presente, conta produtos cujo nome contenha o valor do filtro,
	 * ignorando maiúsculas/minúsculas. Caso contrário, conta todos os produtos.
	 * 
	 * @param filter filtro opcional para contagem por nome do produto
	 * @return o número total de produtos que correspondem ao filtro
	 */
	@Override
	public long countAnyMatching(Optional<String> filter) {
		if (filter.isPresent()) {
			String repositoryFilter = "%" + filter.get() + "%";
			return productRepository.countByNameLikeIgnoreCase(repositoryFilter);
		} else {
			return count();
		}
	}

	/**
	 * Busca todos os produtos paginados.
	 * 
	 * @param pageable objeto para paginação dos resultados
	 * @return uma página contendo produtos
	 */
	public Page<Product> find(Pageable pageable) {
		return productRepository.findBy(pageable);
	}

	/**
	 * Retorna o repositório JPA associado para operações CRUD.
	 * 
	 * @return o repositório {@link JpaRepository} para {@link Product}
	 */
	@Override
	public JpaRepository<Product, Long> getRepository() {
		return productRepository;
	}

	/**
	 * Cria uma nova instância de {@link Product}.
	 * 
	 * @param currentUser o usuário atualmente autenticado (não utilizado neste método)
	 * @return uma nova instância vazia de {@link Product}
	 */
	@Override
	public Product createNew(User currentUser) {
		return new Product();
	}

	/**
	 * Salva o produto fornecido no banco de dados.
	 * 
	 * <p>Captura exceções de violação de integridade de dados para garantir
	 * que o nome do produto seja único, lançando uma exceção amigável
	 * ao usuário caso haja conflito.</p>
	 * 
	 * @param currentUser o usuário atualmente autenticado
	 * @param entity o produto a ser salvo
	 * @return o produto salvo
	 * @throws UserFriendlyDataException se houver conflito de nome duplicado
	 */
	@Override
	public Product save(User currentUser, Product entity) {
		try {
			return FilterableCrudService.super.save(currentUser, entity);
		} catch (DataIntegrityViolationException e) {
			throw new UserFriendlyDataException(
					"There is already a product with that name. Please select a unique name for the product.");
		}
	}
}
