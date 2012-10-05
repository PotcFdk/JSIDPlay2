package applet.entities.gamebase.service;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import applet.entities.gamebase.Games;

public class GamesService {
	private EntityManager em;

	public GamesService(EntityManager em) {
		this.em = em;
	};

	public List<Games> select(char firstLetter) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Games> query = cb.createQuery(Games.class);
		Root<Games> games = query.from(Games.class);
		Path<String> name = games.<String> get("name");
		final Predicate predicate;
		if (Character.isLetter(firstLetter)) {
			predicate = cb.like(cb.lower(name),
					Character.toLowerCase(firstLetter) + "%");
		} else {
			predicate = cb.not(cb.between(cb.lower(name), "a%", "z%"));
		}
		query.where(predicate);
		query.orderBy(cb.asc(name));
		query.select(games);
		return em.createQuery(query).getResultList();
	}
}
