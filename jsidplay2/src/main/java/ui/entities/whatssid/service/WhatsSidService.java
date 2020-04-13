package ui.entities.whatssid.service;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import libsidutils.fingerprinting.FingerPrinting;
import libsidutils.fingerprinting.ini.IniFingerprintConfig;
import libsidutils.fingerprinting.rest.FingerPrintingDataSource;
import libsidutils.fingerprinting.rest.beans.HashBean;
import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.IntArrayBean;
import libsidutils.fingerprinting.rest.beans.SongNoBean;
import sidplay.fingerprinting.MusicInfoBean;
import sidplay.fingerprinting.MusicInfoWithConfidenceBean;
import sidplay.fingerprinting.WavBean;
import ui.entities.whatssid.HashTable;
import ui.entities.whatssid.HashTable_;
import ui.entities.whatssid.MusicInfo;
import ui.entities.whatssid.MusicInfo_;

public class WhatsSidService implements FingerPrintingDataSource {

	private IniFingerprintConfig fingerprintConfig = new IniFingerprintConfig();

	private EntityManager em;

	public WhatsSidService(EntityManager em) {
		this.em = em;
	}

	@Override
	public IdBean insertTune(MusicInfoBean musicInfoBean) {
		// INSERT INTO `MusicInfo` (`Title`, `Artist`, `Album`, `FileDir`, `InfoDir`,
		// audio_length) VALUES

		MusicInfo musicInfo = new MusicInfo();
		musicInfo.setSongNo(musicInfoBean.getSongNo());
		musicInfo.setTitle(musicInfoBean.getTitle());
		musicInfo.setArtist(musicInfoBean.getArtist());
		musicInfo.setAlbum(musicInfoBean.getAlbum());
		musicInfo.setFileDir(musicInfoBean.getFileDir());
		musicInfo.setInfoDir(musicInfoBean.getInfoDir());
		musicInfo.setAudioLength(musicInfoBean.getAudioLength());

		try {
			em.getTransaction().begin();
			em.persist(musicInfo);
			em.getTransaction().commit();
			return new IdBean(musicInfo.getIdMusicInfo());
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
			em.getTransaction().begin();
			for (HashBean hashBean : hashes.getHashes()) {
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

	@Override
	public MusicInfoBean findTune(SongNoBean songNoBean) {
		// SELECT Title, Artist, Album, audio_length FROM `MusicInfo` WHERE
		// idMusicInfo=songNoBean.getSongNo()

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MusicInfo> query = cb.createQuery(MusicInfo.class);
		Root<MusicInfo> musicInfo = query.from(MusicInfo.class);
		Path<Integer> idMusicInfo = musicInfo.<Integer>get(MusicInfo_.idMusicInfo);
		Predicate predicate = cb.equal(idMusicInfo, songNoBean.getSongNo());
		query.where(predicate).select(musicInfo);

		return em.createQuery(query).getSingleResult().toBean();
	}

	@Override
	public HashBeans findHashes(IntArrayBean intArrayBean) {
		// SELECT * FROM `HashTable` WHERE Hash in()

		HashBeans result = new HashBeans();
		if (intArrayBean.getHash() == null || intArrayBean.getHash().length == 0) {
			return result;
		}
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

	@Override
	public boolean tuneExists(MusicInfoBean musicInfoBean) {
		// SELECT * FROM MusicInfo WHERE Title=title, Artist=artist, Album=album,
		// FileDir=fileDir, InfoDir=infoDir

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MusicInfo> query = cb.createQuery(MusicInfo.class);
		Root<MusicInfo> musicInfo = query.from(MusicInfo.class);

		Path<Integer> songNo = musicInfo.<Integer>get(MusicInfo_.songNo);
		Predicate songNoPredicate = cb.equal(songNo, musicInfoBean.getSongNo());

		Path<String> title = musicInfo.<String>get(MusicInfo_.title);
		Predicate titlePredicate = cb.equal(title, musicInfoBean.getTitle());

		Path<String> artist = musicInfo.<String>get(MusicInfo_.artist);
		Predicate artistPredicate = cb.equal(artist, musicInfoBean.getArtist());

		Path<String> album = musicInfo.<String>get(MusicInfo_.album);
		Predicate albumPredicate = cb.equal(album, musicInfoBean.getAlbum());

		Path<String> fileDir = musicInfo.<String>get(MusicInfo_.fileDir);
		Predicate fileDirPredicate = cb.equal(fileDir, musicInfoBean.getFileDir());

		Path<String> infoDir = musicInfo.<String>get(MusicInfo_.infoDir);
		Predicate infoDirPredicate = cb.equal(infoDir, musicInfoBean.getInfoDir());

		query.where(cb.and(songNoPredicate, titlePredicate, artistPredicate, albumPredicate, fileDirPredicate,
				infoDirPredicate)).select(musicInfo);

		return !em.createQuery(query).getResultList().isEmpty();
	}

	@Override
	public MusicInfoWithConfidenceBean whatsSid(WavBean wavBean) throws IOException {
		return new FingerPrinting(fingerprintConfig, this).match(wavBean);
	}

	public void deleteAll() {
		try {
			em.getTransaction().begin();

			CriteriaBuilder cb = em.getCriteriaBuilder();

			CriteriaDelete<MusicInfo> criteriaDeleteMusicInfo = cb.createCriteriaDelete(MusicInfo.class);
			criteriaDeleteMusicInfo.from(MusicInfo.class);
			em.createQuery(criteriaDeleteMusicInfo).executeUpdate();

			CriteriaDelete<HashTable> criteriaDeleteHashTable = cb.createCriteriaDelete(HashTable.class);
			criteriaDeleteHashTable.from(HashTable.class);
			em.createQuery(criteriaDeleteHashTable).executeUpdate();

			em.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}
	}

	public void close() {
		em.close();
	}

}
