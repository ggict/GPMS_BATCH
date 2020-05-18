package kr.go.gg.gpms.ftp.kgGis;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 도로대장 FTP 서버 VO
 */
@Configuration
@ConfigurationProperties(prefix = "ftp.kggis")
public class FtpKgGisVO {

    /** 서버 IP  */
	private String server;

	/** 서버 포트 */
	private int port;

	/** 서버 로그인 계정 */
    private String username;

    /** 서버 로그인 비밀번호 */
    private String password;

    /** 연결 유지 시간 */
    private int keepAliveTimeout;

    /** 자동시작(?) */
    private boolean autoStart;

    /** SFTP 사용여부 */
    private boolean useSecurity;

    /** SFTP 사용여부 */
    private String mode;

    /** 서버 SHP파일 디렉토리 경로 */
    private String remoteShpDirectoryPath;

    /** 로컬 SHP파일 다운로드 디렉토리 경로 */
    private String localShpDownloadDirectoryPath;

    /** 로컬 SHP파일 디렉토리 경로 */
    private String localShpDirectoryPath;

    /** 병합 SHP파일 디렉토리 경로 */
    private String mergedShpFilePath;

    /** 병합 SHP파일 파일 명(확장자 포함) */
    private String mergedShpFileName;

    /** 서버 중심선 SHP파일 파일 명(확장자 미포함) */
    private String remoteLineShpFileName;

    /** 로컬 중심선 SHP파일 파일 명(확장자 미포함) */
    private String localLineShpFileName;

    @PostConstruct
    public void init() {
        if ( port == 0 ) {
            port = 21;
        }

//        if( useSecurity ) {
//        	port = 22;
//        } else {
//        	port = 21;
//        }
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public void setKeepAliveTimeout(int keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

	public boolean isUseSecurity() {
		return useSecurity;
	}

	public void setUseSecurity(boolean useSecurity) {
		this.useSecurity = useSecurity;
	}

	public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getRemoteShpDirectoryPath() {
		return remoteShpDirectoryPath;
	}

	public void setRemoteShpDirectoryPath(String remoteShpDirectoryPath) {
		this.remoteShpDirectoryPath = remoteShpDirectoryPath;
	}

	public String getLocalShpDownloadDirectoryPath() {
        return localShpDownloadDirectoryPath;
    }

    public void setLocalShpDownloadDirectoryPath(String localShpDownloadDirectoryPath) {
        this.localShpDownloadDirectoryPath = localShpDownloadDirectoryPath;
    }

    public String getLocalShpDirectoryPath() {
		return localShpDirectoryPath;
	}

	public void setLocalShpDirectoryPath(String localShpDirectoryPath) {
		this.localShpDirectoryPath = localShpDirectoryPath;
	}

	public String getMergedShpFileName() {
		return mergedShpFileName;
	}

	public void setMergedShpFileName(String mergedShpFileName) {
		this.mergedShpFileName = mergedShpFileName;
	}

	public String getMergedShpFilePath() {
		return mergedShpFilePath;
	}

	public void setMergedShpFilePath(String mergedShpFilePath) {
		this.mergedShpFilePath = mergedShpFilePath;
	}

    public String getRemoteLineShpFileName() {
        return remoteLineShpFileName;
    }

    public void setRemoteLineShpFileName(String remoteLineShpFileName) {
        this.remoteLineShpFileName = remoteLineShpFileName;
    }

    public String getLocalLineShpFileName() {
        return localLineShpFileName;
    }

    public void setLocalLineShpFileName(String localLineShpFileName) {
        this.localLineShpFileName = localLineShpFileName;
    }


}