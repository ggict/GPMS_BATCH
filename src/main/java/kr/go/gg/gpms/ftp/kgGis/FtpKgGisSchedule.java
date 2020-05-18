package kr.go.gg.gpms.ftp.kgGis;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import me.saro.commons.ftp.FTP;
import me.saro.commons.ftp.FTPS;

@Component
public class FtpKgGisSchedule {

    private static final Logger logger = LoggerFactory.getLogger(FtpKgGisSchedule.class);

    @Autowired
    private FtpKgGisVO ftpKgGisVO;

    /**
     * 도로대장 SHP파일 가져오기.
     */
    @Scheduled(cron = "${ftp.kggis.cron.time}")
    public void execute() {
        // FTP 서버 접속하여 파일 가져오기.
        ftpDownload();
        // 가져온 SHP파일 병합하기.(노선별 거리포인트)
        shpFileMerge();
        // 파일 위치 이동하기.
        moveFile();
    }

    /**
     * FTP 서버 접속하여 도로대장 SHP파일 가져오기
     */
    public void ftpDownload() {
        try {
            logger.info("=== [START] Schedule ===");

            // FTP Server Info
            // 서버 IP
            String host = ftpKgGisVO.getServer();
            // 서버 포트
            int port = ftpKgGisVO.getPort();
            // 서버 로그인 계정
            String user = ftpKgGisVO.getUsername();
            // 서버 로그인 비밀번호
            String pass = ftpKgGisVO.getPassword();

            // FTP File Download Path Info
            // 서버 SHP파일 디렉토리 경로
            String remotePath = ftpKgGisVO.getRemoteShpDirectoryPath();
            // 로컬 SHP파일 다운로드 디렉토리 경로
            File localPathFile = new File(ftpKgGisVO.getLocalShpDownloadDirectoryPath());
            // 로컬 디렉토리 없을 경우 생성
            if ( !localPathFile.exists() ) localPathFile.mkdirs();
            // 병합 SHP파일 파일 명
            String mergeFileName = ftpKgGisVO.getMergedShpFileName();

            logger.info("=== [START] Connected to SFTP Server (" + user + "@" + host + ":" + port + ") ===");

            FTP ftp;
            if ( ftpKgGisVO.isUseSecurity() ) {  // SFTP 여부
                // SFTP로 접속
                ftp = FTP.openSFTP(host, port, user, pass);
            } else {
                // FTP로 접속
                ftp = FTP.openFTP(host, port, user, pass);
                // mode가 active일 경우 변경(default : passive)
                if ( "active".equals(ftpKgGisVO.getMode()) ) {
                    ((FTPS)ftp).enterLocalActiveMode();
                }
            }
//	        FTPS ftp;
//	        ftp = new FTPS(InetAddress.getByName(host), port, user, pass, false);
//	        ftp.enterLocalActiveMode();

            logger.info("=== [Current Path] " + ftp.path() + " ===");
            logger.info("=== [Move Path] " + remotePath + " ===");

            // 파일 위치로 이동
            ftp.path(remotePath);

            // 로컬 SHP파일 디렉토리 내에 파일 삭제
            deleteDirectory(localPathFile);

            // 서버 디렉토리 내 파일 다운로드
            ftp.listFiles().forEach(e -> {
                // 서버 디렉토리 내 파일 다운로드
                downloadFile(ftp, e, localPathFile);
            });

            // 서버 하위 디렉토리 목록
            ftp.listDirectories().forEach(e -> {
                try {
                    // 하위 디렉토리 경로 이동
                    ftp.path(remotePath + "/" + e);
                    logger.info("=== [Current Path] " + remotePath + "/" + e + " ===");
                    // 하위 디렉토리 파일 목록
                    ftp.listFiles().forEach(e1 -> {
                        if ( e1.indexOf(FilenameUtils.getBaseName(mergeFileName)) >= 0 ) {
                            // 하위 디렉토리 파일 다운로드
                            downloadFile(ftp, e1, new File(localPathFile + "/" + e));
                        }
                    });
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });

            // FTP 종료
            ftp.close();
            logger.info("=== [END  ] Connected to SFTP Server (" + user + "@" + host + ":" + port + ") ===");
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * 다운받은 SHP파일 병합하기(노선별 거리포인트)
     */
    public void shpFileMerge() {
        try {
            // 로컬 SHP파일 다운로드 디렉토리 경로
            File localPathFile = new File(ftpKgGisVO.getLocalShpDownloadDirectoryPath());

            // 병합 SHP파일 디렉토리 경로
            String mergePath = ftpKgGisVO.getMergedShpFilePath();
            // 병합 SHP파일 디렉토리 없을 경우 생성
            File mf = new File(mergePath);
            if ( !mf.exists() ) mf.mkdirs();
            // 병합 SHP파일 파일 명
            String mergeFileName = ftpKgGisVO.getMergedShpFileName();

            logger.info("=== [START] Creating Result Shape File (" + mergePath + "/" + mergeFileName + ") ===");

            // download한 shp 파일 목록
            List<File> shpFileList = new ArrayList<File>();
            for ( File file : localPathFile.listFiles() ) {
                // 하위 디렉토리별로 파일 검색
                if ( file.isDirectory() ) {
                    File[] f = (new File(file.getPath())).listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.toLowerCase().endsWith("shp");
                        }
                    });
                    shpFileList.addAll(Arrays.asList(f));
                }
            }

            // List에서 Array로 변환
            File[] shpFiles = shpFileList.toArray(new File[shpFileList.size()]);
//            File[] shpFiles = localPathFile.listFiles(new FilenameFilter() {
//                @Override
//                public boolean accept(File dir, String name) {
//                    return name.toLowerCase().endsWith("shp");
//                }
//            });

            // 병합 SHP파일
            File resFile = new File(mergePath + "/" + mergeFileName);
            resFile.createNewFile();

            FileDataStoreFactorySpi factory = new ShapefileDataStoreFactory();
            DataStore resData = factory.createNewDataStore(Collections.singletonMap("url", resFile.toURI().toURL()));
//            FileDataStore resData = FileDataStoreFinder.getDataStore(resFile);
            String rTypeName = resData.getTypeNames()[0];

            Map<String, Object> map2 = new HashMap<>();
            map2.put("url", shpFiles[0].toURI().toURL());
            DataStore readStore2 = DataStoreFinder.getDataStore(map2);
            String resFeatureTypeName;

            if(readStore2 != null && readStore2.getTypeNames().length > 0 && resData.getTypeNames().length > 0) {
                resFeatureTypeName = readStore2.getTypeNames()[0];
                resData.createSchema(readStore2.getSchema(resFeatureTypeName));
            }

            logger.info("=== [END  ] Creating Result Shape File (" + mergePath + "/" + mergeFileName + ") ===");

            FeatureWriter<SimpleFeatureType, SimpleFeature> fw = resData.getFeatureWriterAppend(rTypeName, Transaction.AUTO_COMMIT);
            for(File f : shpFiles) {
                logger.info("=== [START] Append Feature ('" + f.getPath() + "/" + f.getName() + "' => '"+ ftpKgGisVO.getMergedShpFileName() + "') ===");
                Map<String, Object> map = new HashMap<>();
                map.put("url", f.toURI().toURL());
                DataStore readStore = DataStoreFinder.getDataStore(map);

                if(readStore != null && readStore.getTypeNames().length > 0 && resData.getTypeNames().length > 0) {
                    resFeatureTypeName = readStore.getTypeNames()[0];
                    resData.createSchema(readStore.getSchema(resFeatureTypeName));
                }
                else {
                    throw new Exception();
                }

                Query query = new Query(resFeatureTypeName, Filter.INCLUDE);

                FeatureReader<SimpleFeatureType, SimpleFeature> fr = readStore.getFeatureReader(query, Transaction.AUTO_COMMIT);
                int featureCnt = 0;
                while(fr.hasNext()) {
                    SimpleFeature feature = fr.next();
                    SimpleFeature nFeature = fw.next();

                    nFeature.setAttributes(feature.getAttributes());
                    fw.write();
                    featureCnt++;
                }
                fr.close();
                readStore.dispose();
                logger.info("=== [END ] Append Feature ('" + f.getName() + "' => '"+ ftpKgGisVO.getMergedShpFileName() + "') [" + featureCnt + "] ===");
            }
            fw.close();
            readStore2.dispose();

            if ( resFile.length() < (1024 * 10) ) {  // 10Kb보다 작다면 다시 시도
                Thread.sleep(3 * 1000);
                shpFileMerge();
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        logger.info("=== [END  ] Schedule ===");
    }

    /**
     * 로컬 SHP파일 디렉토리 내에 파일 삭제
     * @param localPathFile
     */
    private void deleteDirectory(File localPathFile) {
        logger.info("=== [START] Delete LocalFiles in (" + localPathFile + ") ===");
        try {
            if (localPathFile.exists()) {
                File[] folder_list = localPathFile.listFiles(); // 파일리스트 얻어오기

                for (int i = 0; i < folder_list.length; i++) {
                    if (folder_list[i].isFile()) {
                        folder_list[i].delete();
//                        System.out.println("파일이 삭제되었습니다.");
                    } else {
                        deleteDirectory(folder_list[i]); // 재귀함수호출
//                        System.out.println("폴더가 삭제되었습니다.");
                    }
                    folder_list[i].delete();
                }
                localPathFile.delete(); // 폴더 삭제
            }
        } catch ( Exception e ) {
            e.getStackTrace();
        }
        logger.info("=== [END  ] Delete LocalFiles in (" + localPathFile + ") ===");
    }

    /**
     * 서버에서 FTP로 파일 받기
     * @param ftp
     * @param downloadFileName
     * @param localPathFile
     */
    private void downloadFile(FTP ftp, String downloadFileName, File localPathFile) {
        try {
            if ( !localPathFile.exists() ) {
                localPathFile.mkdirs();
            }
            // 로컬 파일 저장 경로
            String fileFullPath = localPathFile + "/" + downloadFileName;

            // 파일이 중복일 경우 파일명에 순번 입력
//			int dup = 0;
//			while(df.exists()) {
//				df = new File(localPathFile + "/" + FilenameUtils.getBaseName(fileFullPath) + "_" + ++dup + "." + FilenameUtils.getExtension(fileFullPath));
//			}
            if ( ftpKgGisVO.isUseSecurity() ) {
                File df = new File(fileFullPath);
                ftp.recv(downloadFileName, df);
            } else {
                FileOutputStream fout = new FileOutputStream(fileFullPath);
                ((FTPS)ftp).getFtp().retrieveFile(ftp.path() + "/" + downloadFileName, fout);
                fout.close();
            }

            logger.info("=== [Download File] " + downloadFileName + " ===");
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * 파일 위치 이동하기.
     */
    private void moveFile() {
        try {
            // ###########################################################
            // ## 중심선 파일 복사
            // ###########################################################
            // 로컬 SHP파일 다운로드 디렉토리 경로(도로 중심선 경로)
            String localShpDownloadDirectoryPath = ftpKgGisVO.getLocalShpDownloadDirectoryPath();
            // 서버 중심선 SHP파일 파일 명(확장자 미포함)
            String remoteLineShpFileName = ftpKgGisVO.getRemoteLineShpFileName();
            // 로컬 SHP파일 디렉토리 경로(도로 중심선 경로)
            String localShpDirectoryPath = ftpKgGisVO.getLocalShpDirectoryPath();
            // 로컬 중심선 SHP파일 파일 명(확장자 미포함)
            String localLineShpFileName = ftpKgGisVO.getLocalLineShpFileName();

            File lineFiles = new File(localShpDownloadDirectoryPath);

            if ( lineFiles.isDirectory() ) {
                FilenameFilter filter = new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith(remoteLineShpFileName);
                    }
                };
                for ( File file : lineFiles.listFiles(filter) ) {
                    String ext = FilenameUtils.getExtension(file.getName());
                    FileUtils.copyFile(file, new File(localShpDirectoryPath, localLineShpFileName + "." + ext));
                }
            }
            // ###########################################################

            // ###########################################################
            // ## 병합 SHP 파일 복사
            // ###########################################################
            // 병합 SHP파일 디렉토리 경로(노선별 거리포인트 경로)
            String mergePath = ftpKgGisVO.getMergedShpFilePath();
            // 병합 SHP파일 파일 명(노선별 거리포인트 파일명)
            String mergeFileName = FilenameUtils.getBaseName(ftpKgGisVO.getMergedShpFileName());

            File mergeFiles = new File(mergePath);

            if ( mergeFiles.isDirectory() ) {
                FilenameFilter filter = new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith(mergeFileName);
                    }
                };
                for ( File file : mergeFiles.listFiles(filter) ) {
                    FileUtils.copyFile(file, new File(localShpDirectoryPath, file.getName()));
                }
            }
            // ###########################################################

            logger.info("=== [Move File] ===");
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}
