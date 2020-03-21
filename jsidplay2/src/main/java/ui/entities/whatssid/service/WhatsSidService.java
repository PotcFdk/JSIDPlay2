package ui.entities.whatssid.service;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

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

public class WhatsSidService {

	private EntityManager em;

	public WhatsSidService(EntityManager em) {
		this.em = em;
	};

	public IdBean insertTune(MusicInfoBean musicInfoBean) {
		// INSERT INTO `MusicInfo`
		// (`Title`, `Artist`, `Album`, `FileDir`, `InfoDir`, audio_length)
		// VALUES ( ? , ? , ? , ? , ? , ? );

		MusicInfo musicInfo = new MusicInfo();
		musicInfo.setTitle(musicInfoBean.getTitle());
		musicInfo.setArtist(musicInfoBean.getArtist());
		musicInfo.setAlbum(musicInfoBean.getAlbum());
		musicInfo.setFileDir(musicInfoBean.getFileDir());
		musicInfo.setInfoDir(musicInfoBean.getInfoDir());
		musicInfo.setAudioLength(musicInfoBean.getAudioLength());

		IdBean result = null;
		try {
			em.getTransaction().begin();
			em.persist(musicInfo);
			em.getTransaction().commit();
			// TODO int / long?
			result = new IdBean();
			result.setId((int) musicInfo.getIdMusicInfo());
		} catch (Exception e) {
			e.printStackTrace();
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}
		return result;
	}

	public void insertHashes(List<HashBean> hashBeans) {
		// INSERT INTO `HashTable` (`Hash`, `id`, `Time`) VALUES
		try {
			em.getTransaction().begin();
			for (HashBean hashBean : hashBeans) {
				HashTable hashTable = new HashTable();
				hashTable.setHash(hashBean.getHash());
				hashTable.setId(hashBean.getId());
				hashTable.setTime(hashBean.getTime());
				em.persist(hashTable);
			}
			em.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}

	}

	public MusicInfoBean findTune(SongNoBean songNoBean) {
		// SELECT Title, Artist, Album, audio_length
		// FROM `MusicInfo` WHERE idMusicInfo=songNoBean.getSongNo()
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MusicInfo> query = cb.createQuery(MusicInfo.class);
		Root<MusicInfo> musicInfo = query.from(MusicInfo.class);
		Path<Long> idMusicInfo = musicInfo.<Long>get(MusicInfo_.idMusicInfo);
		Predicate predicate = cb.equal(idMusicInfo, songNoBean.getSongNo());
		query.where(predicate).select(musicInfo);

		return em.createQuery(query).getSingleResult().toBean();
	}

	public HashBeans findHash(IntArrayBean intArrayBean) {
		// SELECT * FROM `HashTable` WHERE Hash in()
		HashBeans result = new HashBeans();
		result.setHashes(new ArrayList<>());

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<HashTable> query = cb.createQuery(HashTable.class);
		Root<HashTable> hashTable = query.from(HashTable.class);
		In<Integer> in = cb.in(hashTable.<Integer>get(HashTable_.hash));
		for (int hash : intArrayBean.getHash()) {
			in.value(hash);
		}
		query.where(in).select(hashTable);

		em.createQuery(query).getResultList().stream().map(hash -> hash.toBean())
				.forEach(hashBean -> result.getHashes().add(hashBean));
		return result;
	}

}
