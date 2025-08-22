package org.example.rabbitmq_server.controller;

import com.sun.javafx.scene.control.Properties;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.Optional;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import org.example.SQlite.DataBaseService;
import org.example.rabbitmq_server.model.User;
import org.example.rabbitmq_server.propReader;
import org.example.rabbitmq_server.service.RabbitMQService;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.time.LocalDateTime;

public class MainController implements Initializable {

    @FXML
    private TextField usernameField;

    @FXML
    private TableColumn<?, ?> actionColumn;

    @FXML
    private TableColumn<?, ?> configureRegexpColumn;

    @FXML
    private TableColumn<?, ?> readRegexpColumn;

    @FXML
    private TableColumn<?, ?> vhostColumn;

    @FXML
    private TableColumn<?, ?> writeRegexpColumn;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button logoutButton;

    @FXML
    private TextField ipAddressField;

    @FXML
    private TextField perUserNameField;

    @FXML
    private TextField readField;

    @FXML
    private Button savePermissionButton;

    @FXML
    private TextField configureField;

    @FXML
    private TextField writeField;

    @FXML
    private TextField VHostField;

    @FXML
    private TextField portField;

    @FXML
    private TextField sslPortField;

    @FXML
    private TextField managementPortField;

    @FXML
    private Button saveConfigButton;

    @FXML
    private Label statusLabel;

    @FXML
    private Label currentUserLabel;

    @FXML
    private Button startServerButton;

    @FXML
    private Button stopServerButton;

    @FXML
    private Button refreshUsersButton;

    @FXML
    private TableView<User> usersTable;

    @FXML
    private TableColumn<User, String> nameColumn;

    @FXML
    private TableColumn<User, String> tagsColumn;

    @FXML
    private TableColumn<User, String> logInDate;

    @FXML
    private TableColumn<User, String> logOutDate;

    @FXML
    private TableColumn<User, String> sessionDuration;

    @FXML
    private TextField newUsernameField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private ComboBox<String> userTypeComboBox;

    @FXML
    private Button createUserButton;

    @FXML
    private Button deleteUserButton;

    @FXML
    private TabPane mainTabPane;

    private RabbitMQService rabbitMQService;
    private final ObservableList<User> users;
    private boolean isLoggedIn = false;
    private Timeline sessionUpdateTimer;
    private LocalDateTime currentUserLoginTime;
    private propReader props;

    public MainController() throws FileNotFoundException {
        this.rabbitMQService = new RabbitMQService();
        this.users = FXCollections.observableArrayList();
        this.props = new propReader();
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupComboBox();
        setupInitialState();
        setSessionUpdateTimer();
        loadConfigFromFile();
        
        rabbitMQService.updateSessionDurationInDatabase();

        
        usersTable.setItems(users);
        
        loginButton.setOnAction(e -> handleLogin());
        logoutButton.setOnAction(e -> handleLogout());
        
        startServerButton.setOnAction(e -> handleStartServer());
        stopServerButton.setOnAction(e -> handleStopServer());
        refreshUsersButton.setOnAction(e -> handleRefreshUsers());
        
        createUserButton.setOnAction(e -> handleCreateUser());
        deleteUserButton.setOnAction(e -> handleDeleteUser());

        saveConfigButton.setOnAction(e -> {
            try {
                handleSaveConfig();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        savePermissionButton.setOnAction(e -> {
            try {
                setPermissionUser();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

    }

    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        tagsColumn.setCellValueFactory(new PropertyValueFactory<>("tags"));
        logInDate.setCellValueFactory(new PropertyValueFactory<>("lastLoginFormatted"));
        logOutDate.setCellValueFactory(new PropertyValueFactory<>("lastLogoutFormatted"));
        sessionDuration.setCellValueFactory(new PropertyValueFactory<>("sessionDurationFormatted"));
    }

    private void setupComboBox() {
        userTypeComboBox.setItems(FXCollections.observableArrayList(
                "none", "monitoring", "policymaker", "management", "administrator"
        ));
        userTypeComboBox.setValue("none");
    }

    private void setupInitialState(){
        setLoggedInState(false);
        statusLabel.setText("Bağlantı bekleniyor...");
        currentUserLabel.setText("Giriş yapılmadı");
    }

    private void setSessionUpdateTimer(){
        sessionUpdateTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateActiveSessions()));
        sessionUpdateTimer.setCycleCount(Timeline.INDEFINITE);
    }

    // Kullanıcı izin ayarları
    private void setPermissionUser() throws Exception {
        String username = perUserNameField.getText();
        String configure = configureField.getText();
        String write = writeField.getText();
        String read = readField.getText();

        if(username.isEmpty()){
            showAlert(Alert.AlertType.ERROR, "Hata", "Kullanıcı adı boş bırakılamaz");
        }

        rabbitMQService.setPermissions(username, configure, write, read);

        perUserNameField.clear();
        configureField.clear();
        writeField.clear();
        readField.clear();

    }

    // Kullanıcı giriş yapmasına göre arayüz ayarları (Yetki kontrolüne görede arayüz ayarları yapılyor)
    private void setLoggedInState(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;

        usernameField.setDisable(isLoggedIn);
        passwordField.setDisable(isLoggedIn);
        loginButton.setDisable(isLoggedIn);
        logoutButton.setDisable(!isLoggedIn);

        mainTabPane.setDisable(!isLoggedIn);

        if(isLoggedIn){
            currentUserLabel.setText("Kullanıcı: " + usernameField.getText());
            statusLabel.setText("Bağlantı Başarılı");

            currentUserLoginTime = LocalDateTime.now();

            if(sessionUpdateTimer != null){
                sessionUpdateTimer.play();
            }

            handleRefreshUsers();
            checkAdminPermissions();
        }else{
            currentUserLabel.setText("Giriş yapılmadı");
            statusLabel.setText("Bağlantı kesildi");

            if(sessionUpdateTimer != null){
                sessionUpdateTimer.stop();
            }

            users.clear();
        }
    }

    // Admin yetkilerini kontrol edip buna göre butonları aktif/pasif yapma
    private void checkAdminPermissions() {
        boolean isAdmin = rabbitMQService.isCurrentUserAdmin();

        createUserButton.setDisable(!isAdmin);
        deleteUserButton.setDisable(!isAdmin);

        if(!isAdmin){
            showAlert(Alert.AlertType.WARNING, "Uyarı",
                    "Yönetici olmadığınız için bazı özellikler kısıtlıdır.");
        }
    }

    // Kullanıcı giriş ve çıkış metotları
    @FXML
    private void handleLogin(){
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if(username.isEmpty() || password.isEmpty()){
            showAlert(Alert.AlertType.ERROR, "Hata", "Kullanıcı adı ve şifre gereklidir!");
            return;
        }

        statusLabel.setText("Bağlanıyor");

        new Thread(() -> {
            try{
                rabbitMQService.setCredentials(username, password);
                boolean connected = rabbitMQService.connection();

                Platform.runLater(() -> {
                    if (connected){
                        rabbitMQService.recordUserLogin(username,"hashed_" + password.hashCode());
                        setLoggedInState(true);
                    }else{
                        statusLabel.setText("RabbitMQ çalışmıyor");
                        showAlert(Alert.AlertType.WARNING, "RabbitMQ Durumu", 
                            "RabbitMQ sunucusu çalışmıyor.\n\n" +
                            "Başlatmak için:\n" +
                            "1. 'Server Başlat' butonuna basın\n" +
                            "2. Veya manuel: net start RabbitMQ\n" +
                            "3. Sonra tekrar giriş yapmayı deneyin");
                    }
                });
            }catch (Exception e){
                Platform.runLater(() -> {
                    statusLabel.setText("Bağlantı hatası");
                    showAlert(Alert.AlertType.ERROR, "Bağlantı Hatası", 
                        "RabbitMQ bağlantısı kurulamadı.\n\n" +
                        "Kontrol edin:\n" +
                        "1. RabbitMQ servisi çalışıyor mu?\n" +
                        "2. Kullanıcı adı/şifre doğru mu?\n" +
                        "3. Management plugin aktif mi?");
                });
            }
        }).start();
    }

    @FXML
    private void handleLogout(){
        String username = usernameField.getText();
        if(!username.isEmpty()){
            rabbitMQService.recordUserLogout(username);
        }
        setLoggedInState(false);
        usernameField.clear();
        passwordField.clear();
    }

    // Server başlatma ve durdurma metotları
    @FXML
    private void handleStartServer(){
        statusLabel.setText("Server başlatılıyor...");

        new Thread(() -> {
            try{
                rabbitMQService.startRabbitMQServer();
                Platform.runLater(() -> {
                    statusLabel.setText("Server çalışıyor");
                    showAlert(Alert.AlertType.INFORMATION, "Başarılı", "RabbitMQ server başlatıldı");
                });
            }catch (Exception e){
                Platform.runLater(() -> {
                    statusLabel.setText("Server çalışıyor");
                    showAlert(Alert.AlertType.INFORMATION, "Bilgi", "RabbitMQ başlatma tamamlandı. Manuel kontrol gerekebilir.");
                });
            }
        }).start();
    }

    @FXML
    private void handleStopServer(){
        statusLabel.setText("Server durduruluyor...");

        new Thread(() -> {
            try{
                rabbitMQService.stopRabbitMQServer();
                Platform.runLater(() -> {
                    statusLabel.setText("Server durduruldu");
                    showAlert(Alert.AlertType.INFORMATION, "Başarılı", "RabbitMQ server durduruldu");
                });
            }catch (Exception e){
                Platform.runLater(() -> {
                    statusLabel.setText("Server durduruldu");
                    showAlert(Alert.AlertType.INFORMATION, "Başarılı", "RabbitMQ server durduruldu");
                });
            }
        }).start();
    }

    //Kullanıcı ekleme ve silme metotları
    @FXML
    private void handleCreateUser(){
        String username = newUsernameField.getText().trim();
        String password = newPasswordField.getText();
        String tags = userTypeComboBox.getValue();

        if(username.isEmpty() || password.isEmpty()){
            showAlert(Alert.AlertType.ERROR, "Hata", "Kullanıcı adı ve şifre gerekli...");
            return;
        }

        new Thread(() -> {
            try{
                rabbitMQService.createUser(username, password, tags);
                Platform.runLater(() -> {
                    newUsernameField.clear();
                    newPasswordField.clear();
                    userTypeComboBox.setValue(tags);
                    handleRefreshUsers();
                    showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Kullanıcı oluşturuldu..." + username);
                });
            }catch (Exception e){
                                e.printStackTrace();
                Platform.runLater(() -> {
                    String errorMsg = e.getMessage();
                    if (errorMsg.contains("401")) {
                        errorMsg = "Yetki hatası: Giriş bilgilerinizi kontrol edin";
                    } else if (errorMsg.contains("403")) {
                        errorMsg = "İzin hatası: Admin yetkisi gerekli";
                    }
                    showAlert(Alert.AlertType.ERROR, "Kullanıcı Oluşturma Hatası", errorMsg);
                });
            }
        }).start();
    }

    @FXML
    private void handleDeleteUser(){
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if(selectedUser == null){
            showAlert(Alert.AlertType.WARNING, "Uyarı", "Lütfen silinecek kullanıcıyı seçin...");
            return;
        }

        if(selectedUser.getName().equals("guest")){
            showAlert(Alert.AlertType.ERROR, "Hata", "Guest kullanıcısı silinemez...");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Kullanıcı Silme Onayı");
        confirmAlert.setHeaderText("Kullanıcıyı silmek istediğinizden emin misiniz?");
        confirmAlert.setContentText("Kullanıcı: " + selectedUser.getName());

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if(result.isPresent() && result.get() == ButtonType.OK){
            new Thread(() -> {
                try{
                    rabbitMQService.deleteUser(selectedUser.getName());
                    Platform.runLater(() -> {
                        handleRefreshUsers();
                        showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Kullanıcı silindi..." + selectedUser.getName());
                    });
                }catch (Exception e){
                    Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "Hata", "Kullanıcı silinemedi..." + e.getMessage())
                    );
                }
            }).start();
        }
    }

    // Kullanıcı tablosunu yenileme
    @FXML
    private void handleRefreshUsers() {
        if (!isLoggedIn) return;

        new Thread(() -> {
            try {
                List<User> userList = rabbitMQService.getUsers();
                Platform.runLater(() -> {
                    users.clear();
                    users.addAll(userList);
                    statusLabel.setText("Kullanıcılar yüklendi");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("Kullanıcı yükleme hatası");
                    showAlert(Alert.AlertType.ERROR, "Hata", 
                        "Kullanıcılar yüklenemedi.\n\nDetay: " + e.getMessage() + 
                        "\n\nRabbitMQ Management API'ye erişim izniniz olduğundan emin olun.");
                });
            }
        }).start();
    }

    // Port ayarlarını kaydetme
    @FXML
    private void handleSaveConfig() throws Exception {
        try {
        int portNo = Integer.parseInt(portField.getText());
        int managementPort = Integer.parseInt(managementPortField.getText());
        int sslPort = Integer.parseInt(sslPortField.getText());

            boolean portSuccess = rabbitMQService.setRabbitMQPortSafely(portNo);
            boolean mgmtSuccess = rabbitMQService.setRabbitMQManagementPortSafely(managementPort);
            boolean sslSuccess = rabbitMQService.setRabbitMQSslPortSafely(sslPort);

            if (portSuccess && mgmtSuccess && sslSuccess) {
        updateRabbitMqConfig("", portNo, managementPort, sslPort);
                
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Port Değişikliği");
                confirm.setHeaderText("Port ayarları güncellendi");
                confirm.setContentText("Değişikliklerin geçerli olması için RabbitMQ'yu yeniden başlatmak gerekiyor. Şimdi yeniden başlatsın mı?");
                
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    boolean restartSuccess = rabbitMQService.restartRabbitMQSafely();
                    
                    if (restartSuccess) {
                        showAlert(Alert.AlertType.INFORMATION, "Başarılı", 
                                "Port ayarları başarıyla güncellendi ve RabbitMQ yeniden başlatıldı!");
                        statusLabel.setText("RabbitMQ çalışıyor - Port: " + portNo + " -SSL Port: " + sslPort);
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Kısmi Başarı", 
                                "Port ayarları güncellendi ama RabbitMQ yeniden başlatılamadı. Manuel başlatmayı deneyin.");
                        statusLabel.setText("RabbitMQ durumu belirsiz");
                    }
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "Bilgi", 
                            "Port ayarları güncellendi. Değişikliklerin etkili olması için RabbitMQ'yu manuel olarak yeniden başlatın.");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Hata", 
                        "Port ayarları güncellenirken hata oluştu. Konsol çıktısını kontrol edin.");
            }
            
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Hata", "Geçersiz port numarası! Sadece sayı girin.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Hata", "Beklenmeyen hata: " + e.getMessage());
        }
    }

    // Config dosyasına kaydetme (Var olan ayarı güncelleme yada yok ise ekleme)
    public void updateRabbitMqConfig(String vhost, int port, int managementPort, int sslPort) throws Exception{
        String filePath = props.getValue("rabbitMQ_file_path");
        
        if (filePath == null || filePath.isEmpty()) {
            String userHome = System.getProperty("user.home");
            filePath = userHome + "\\AppData\\Roaming\\RabbitMQ\\rabbitmq.conf";
        }
        
        Path path = Paths.get(filePath);

        List<String> lines = new ArrayList<>();
        if(Files.exists(path)) {
            lines = Files.readAllLines(path);
        }

        lines = UpdateOrAdd(lines , "listeners.tcp.default", String.valueOf(port));
        lines = UpdateOrAdd(lines , "listeners.tcp.1", "127.0.0.1:" + port);
        lines = UpdateOrAdd(lines, "listeners.ssl.default", String.valueOf(sslPort));
        lines = UpdateOrAdd(lines , "management.tcp.port", String.valueOf(managementPort));


        Files.createDirectories(path.getParent());
        Files.write(path, lines, StandardCharsets.UTF_8);
        Platform.runLater(() -> {
            statusLabel.setText("Config güncellendi - restart gerekli");
        });
    }

    private List<String> UpdateOrAdd(List<String> lines, String key, String value){
        boolean updated = false;
        for(int i = 0; i < lines.size(); i++){
            if(lines.get(i).startsWith(key)){
                lines.set(i, key + " = " + value);
                updated = true;
                break;
            }
        }
        if(!updated){
            lines.add(key + " = " +value);
        }
        return lines;
    }

    // Config dosyasındaki ayarları yükleme
    private void loadConfigFromFile(){
        try{
            Path path = Paths.get("C:\\Users\\ahmet\\AppData\\Roaming\\RabbitMQ\\rabbitmq.conf");
            if(Files.exists(path)){
                List<String> lines = Files.readAllLines(path);

                for(String line : lines){
                    line = line.trim();
                    if(line.startsWith("listeners.tcp.default")){
                        String[] parts = line.split("=");
                        if(parts.length == 2){
                            String portStr = parts[1].trim();
                            portField.setText(portStr);
                            rabbitMQService.setRabbitMQPort(Integer.parseInt(portStr));
                        }
                    }

                    if(line.startsWith("listeners.ssl.default")){
                        String[] parts = line.split("=");
                        if(parts.length == 2){
                            String sslPortStr = parts[1].trim();
                            sslPortField.setText(sslPortStr);
                            rabbitMQService.setRabbitMQManagementPort(Integer.parseInt(sslPortStr));
                        }
                    }

                    if(line.startsWith("management.tcp.port")){
                        String[] parts = line.split("=");
                        if(parts.length == 2){
                            String managementPortStr = parts[1].trim();
                            managementPortField.setText(managementPortStr);
                            rabbitMQService.setRabbitMQManagementPort(Integer.parseInt(managementPortStr));
                        }
                    }
                }
            }
        }catch (Exception e){
            statusLabel.setText("Config dosyası okunamadı!!");
            System.out.println("Hata: " + e.getMessage());
        }
    }

    // Aktif oturum bilgilerini güncelleme
    private void updateActiveSessions(){
        if(!isLoggedIn){
            return;
        }
        Platform.runLater(() -> {
           for (User user : users) {
               if(user.isActiveSession()) {
                   String newDuration = calculateLiveDurationForUser(user);
                   user.setSessionDurationFormatted(newDuration);
               }
           }
           usersTable.refresh();
           updateCurrenUserDisplay();
        });
    }

    // Anlık oturum süresi hesaplama
    private String calculateLiveDurationForUser(User user) {
        if (user.getLoginStartTime() == null) return "0 dk";

        java.time.Duration duration = java.time.Duration.between(user.getLoginStartTime(), LocalDateTime.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        if (hours > 0) {
            return String.format("%d sa %d dk %d sn", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d dk %d sn", minutes, seconds);
        } else {
            return String.format("%d sn", seconds);
        }
    }

    // Oturum süresini hesaplama
    private String calculateCurrentUserDuration() {
        if (currentUserLoginTime == null) return "0 dk";

        java.time.Duration duration = java.time.Duration.between(currentUserLoginTime, LocalDateTime.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        if (hours > 0) {
            return String.format("%d sa %d dk %d sn", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d dk %d sn", minutes, seconds);
        } else {
            return String.format("%d sn", seconds);
        }
    }

    // Durum bilgisi güncelleme
    private void updateCurrenUserDisplay(){
        if(currentUserLoginTime != null){
            String currentSessionStatus = calculateCurrentUserDuration();
            statusLabel.setText("Bağlı - Oturum: " + currentSessionStatus);
        }
    }



    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
