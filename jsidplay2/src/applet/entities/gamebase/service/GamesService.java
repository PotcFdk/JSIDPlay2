package applet.entities.gamebase.service;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import applet.entities.gamebase.Games;
import applet.entities.gamebase.Games_;

public class GamesService {
	private EntityManager em;

	public GamesService(EntityManager em) {
		this.em = em;
	};

	public List<Games> select(char firstLetter) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Games> query = cb.createQuery(Games.class);
		Root<Games> games = query.from(Games.class);
		Path<String> name = games.<String> get(Games_.name);
		final Predicate predicate;
		if (Character.isLetter(firstLetter)) {
			predicate = cb.like(cb.lower(name),
					Character.toLowerCase(firstLetter) + "%");
		} else {
			// first character matches everything EXCEPT letters
			predicate = cb.and(cb.not(cb.between(cb.lower(name), "a%", "z%")),
					cb.notLike(cb.lower(name), "a%"),
					cb.notLike(cb.lower(name), "z%"));
		}
		query.where(predicate).orderBy(cb.asc(name)).select(games);
		return em.createQuery(query).getResultList();
	}
}
