package ui.entities.whatssid.service;

import static libsidplay.config.IWhatsSidSystemProperties.QUERY_TIMEOUT;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.annotations.QueryHints;

import libsidutils.fingerprinting.rest.FingerPrintingDataSource;
import libsidutils.fingerprinting.rest.beans.HashBean;
import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.IntArrayBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import libsidutils.fingerprinting.rest.beans.SongNoBean;
import ui.entities.whatssid.HashTable;
import ui.entities.whatssid.HashTable_;
import ui.entities.whatssid.MusicInfo;
import ui.entities.whatssid.MusicInfo_;

public class WhatsSidService implements FingerPrintingDataSource {

	private EntityManager em;

	public WhatsSidService(EntityManager em) {
		this.em = em;
	}

	@Override
	public IdBean insertTune(MusicInfoBean musicInfoBean) {
		// INSERT INTO `MusicInfo` ('songNo', `Title`, `Artist`, `Album`, `FileDir`,
		// `InfoDir`, audio_length) VALUES
		try {
			if (em.isOpen()) {
				em.getTransaction().begin();

				MusicInfo musicInfo = new MusicInfo();
				musicInfo.setSongNo(musicInfoBean.getSongNo());
				musicInfo.setTitle(musicInfoBean.getTitle());
				musicInfo.setArtist(musicInfoBean.getArtist());
				musicInfo.setAlbum(musicInfoBean.getAlbum());
				musicInfo.setFileDir(musicInfoBean.getFileDir());
				musicInfo.setInfoDir(musicInfoBean.getInfoDir());
				musicInfo.setAudioLength(musicInfoBean.getAudioLength());
				em.persist(musicInfo);

				em.getTransaction().commit();
				return new IdBean(musicInfo.getIdMusicInfo());
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}
		return null;
	}

	@Override
	public void insertHashes(HashBeans hashes) {
		// INSERT INTO `HashTable` (`Hash`, `id`, `Time`) VALUES

		if (hashes.getHashes() == null || hashes.getHashes().isEmpty()) {
			return;
		}
		try {
			if (em.isOpen()) {
				em.getTransaction().begin();

				for (HashBean hashBean : hashes.getHashes()) {
					HashTable hashTable = new HashTable();
					hashTable.setHash(hashBean.getHash());
					hashTable.setId(hashBean.getId());
					hashTable.setTime(hashBean.getTime());
					em.persist(hashTable);
				}
				em.getTransaction().commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}

	}

	@Override
	public MusicInfoBean findTune(SongNoBean songNoBean) {
		// SELECT songNo, Title, Artist, Album, audio_length FROM `MusicInfo` WHERE
		// idMusicInfo=songNoBean.getSongNo()
		try {
			if (em.isOpen()) {
				em.getTransaction().begin();

				CriteriaBuilder cb = em.getCriteriaBuilder();
				CriteriaQuery<MusicInfo> query = cb.createQuery(MusicInfo.class);
				Root<MusicInfo> root = query.from(MusicInfo.class);

				query.select(root).where(cb.equal(root.get(MusicInfo_.idMusicInfo), songNoBean.getSongNo()));

				MusicInfoBean result = em.createQuery(query).getSingleResult().toBean();

				em.getTransaction().commit();
				return result;
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}
		return null;
	}

	@Override
	public HashBeans findHashes(IntArrayBean intArrayBean) {
		// SELECT * FROM `HashTable` WHERE Hash in()

		HashBeans result = new HashBeans();
		if (intArrayBean.getHash() == null || intArrayBean.getHash().length == 0) {
			return result;
		}
		try {
			if (em.isOpen()) {
				em.getTransaction().begin();

				CriteriaBuilder cb = em.getCriteriaBuilder();
				CriteriaQuery<HashTable> query = cb.createQuery(HashTable.class);
				Root<HashTable> root = query.from(HashTable.class);

				query.select(root).where(root.get(HashTable_.hash).in((Object[]) intArrayBean.getHash()));

				em.createQuery(query).setHint(QueryHints.READ_ONLY, true).setHint(QueryHints.TIMEOUT_JPA, QUERY_TIMEOUT)
						.getResultList().stream().map(HashTable::toBean).forEach(result.getHashes()::add);

				em.getTransaction().commit();
				return result;
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}
		return null;
	}

	@Override
	public boolean tuneExists(MusicInfoBean musicInfoBean) {
		// SELECT count(*) FROM MusicInfo WHERE songNo=songNo, Title=title AND
		// Artist=artist AND Album=album AND FileDir=fileDir AND InfoDir=infoDir
		try {
			if (em.isOpen()) {
				em.getTransaction().begin();

				CriteriaBuilder cb = em.getCriteriaBuilder();
				CriteriaQuery<Long> query = cb.createQuery(Long.class);
				Root<MusicInfo> root = query.from(MusicInfo.class);

				Predicate songNoPredicate = cb.equal(root.get(MusicInfo_.songNo), musicInfoBean.getSongNo());

				Predicate titlePredicate = cb.equal(root.get(MusicInfo_.title), musicInfoBean.getTitle());

				Predicate artistPredicate = cb.equal(root.get(MusicInfo_.artist), musicInfoBean.getArtist());

				Predicate albumPredicate = cb.equal(root.get(MusicInfo_.album), musicInfoBean.getAlbum());

				Predicate fileDirPredicate = cb.equal(root.get(MusicInfo_.fileDir), musicInfoBean.getFileDir());

				Predicate infoDirPredicate = cb.equal(root.get(MusicInfo_.infoDir), musicInfoBean.getInfoDir());

				query.select(cb.count(root)).where(cb.and(songNoPredicate, titlePredicate, artistPredicate,
						albumPredicate, fileDirPredicate, infoDirPredicate));

				boolean result = em.createQuery(query).getSingleResult() > 0;

				em.getTransaction().commit();
				return result;
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}
		return false;
	}

	public void deleteAll() {
		try {
			if (em.isOpen()) {
				em.getTransaction().begin();

				CriteriaBuilder cb = em.getCriteriaBuilder();

				CriteriaDelete<MusicInfo> criteriaDeleteMusicInfo = cb.createCriteriaDelete(MusicInfo.class);
				criteriaDeleteMusicInfo.from(MusicInfo.class);
				em.createQuery(criteriaDeleteMusicInfo).executeUpdate();

				CriteriaDelete<HashTable> criteriaDeleteHashTable = cb.createCriteriaDelete(HashTable.class);
				criteriaDeleteHashTable.from(HashTable.class);
				em.createQuery(criteriaDeleteHashTable).executeUpdate();

				em.getTransaction().commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}
	}

	public void close() {
		if (em.isOpen()) {
			em.close();
		}
	}

}
