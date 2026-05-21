package ninjamica.tasktwig.core;

import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.util.Duration;
import ninjamica.tasktwig.ui.TaskTwigController;
import ninjamica.tasktwig.ui.util.TimeInput;

import java.time.LocalTime;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

public class SaveSyncService extends ScheduledService<SaveSyncService.SyncState> {

    private final TaskTwigController controller;
    private final TaskTwig twig;
    private final TaskTwig.SyncConflictCallback conflictCallback;
    private final BooleanSupplier userConfirmSave;
    private volatile boolean syncOverrideFlag = false;
    private volatile boolean exitPromptFlag = false;
    private volatile boolean exitPromptAsked = false;

    public enum SyncState {
        SYNC,
        SAVE,
        UPLOAD,
        DOWNLOAD,
        MERGE,
        CONFLICT,
        DONE,
        ERROR,
    }

    public SaveSyncService(TaskTwigController controller, TaskTwig.SyncConflictCallback conflictCallback, BooleanSupplier userConfirmSave) {
        this.controller = controller;
        this.twig = TaskTwig.instance();
        this.conflictCallback = conflictCallback;
        this.userConfirmSave = userConfirmSave;
    }

    public void forceSync() {
        syncOverrideFlag = true;
    }

    public void syncNow() {
        setDelay(Duration.ZERO);
        forceSync();
        restart();
    }

    public void syncAndExit() {
        setOnSucceeded(event -> Platform.exit());
        setOnFailed(event -> Platform.exit());
        exitPromptFlag = true;
        restart();
    }

    @Override
    protected javafx.concurrent.Task<SyncState> createTask() {
        final boolean exitPrompt = exitPromptFlag;
        final boolean forceSync = syncOverrideFlag;
        syncOverrideFlag = false;

        return new javafx.concurrent.Task<>() {
            @Override
            protected SyncState call() {
//                setStartUI();
                updateValue(SyncState.SAVE);

                boolean syncToDbx = forceSync || CompletableFuture.supplyAsync(twig.autoSyncProperty()::get, Platform::runLater).join();

                updateMessage("Saving and hashing data");
                twig.saveToFileFX();

                if (twig.dbxClient().getValue() != null) {
                    updateMessage("Comparing data with Dropbox");
//                    setIconUI(IconState.SYNC, true);
                    updateValue(SyncState.SYNC);

                    TaskTwig.CommitDiff commitDiff;
                    if (syncToDbx) {
                        commitDiff = twig.compareCommitToDbx(conflictCallback);
                    }
                    else if (exitPrompt) {
                        commitDiff = twig.compareCommitToDbx((overallAction, fileActions) -> {
                            exitPromptAsked = true;
                            if (userConfirmSave.getAsBoolean())
                                return conflictCallback.getUserChoice(overallAction, fileActions);
                            else
                                return null;
                        });

                        if (commitDiff.action() != null && commitDiff.action() != TaskTwig.FileAction.NONE) {
                            syncToDbx = exitPromptAsked || userConfirmSave.getAsBoolean();
                        }
                    }
                    else {
                        commitDiff = twig.compareCommitToDbx(null);
                    }

                    if (syncToDbx) {
                        switch (commitDiff.action()) {
                            case UPLOAD -> {
                                updateMessage("Uploading data to Dropbox");
//                                setIconUI(IconState.UPLOAD, true);
                                updateValue(SyncState.UPLOAD);
                            }
                            case DOWNLOAD -> {
                                updateMessage("Downloading data from Dropbox");
//                                setIconUI(IconState.DOWNLOAD, true);
                                updateValue(SyncState.DOWNLOAD);
                                CompletableFuture.runAsync(controller::detachTwigData, Platform::runLater).join();
                            }
                            case MERGE -> {
                                updateMessage("Merging data from Dropbox with local");
//                                setIconUI(IconState.MERGE, true);
                                updateValue(SyncState.MERGE);
                                CompletableFuture.runAsync(controller::detachTwigData, Platform::runLater).join();
                            }
                            case NONE -> {
                                updateMessage("In sync as of " + LocalTime.now().format(TimeInput.timeFormat));
//                                setIconUI(IconState.DONE, false);
                                return SyncState.DONE;
                            }
                        }
                        twig.dbxSync(commitDiff);

//                        setIconUI(IconState.DONE, false);
                        switch (commitDiff.action()) {
                            case UPLOAD -> updateMessage("Synced to remote at " + LocalTime.now().format(TimeInput.timeFormat));
                            case DOWNLOAD -> {
                                Platform.runLater(controller::attachTwigData);
                                updateMessage("Synced from remote at " + LocalTime.now().format(TimeInput.timeFormat));
                            }
                            case MERGE -> {
                                Platform.runLater(controller::attachTwigData);
                                updateMessage("Synced with remote at " + LocalTime.now().format(TimeInput.timeFormat));
                            }
                        }
                        
                        return SyncState.DONE;
                    }
                    else {
                        switch(commitDiff.action()) {
                            case NONE -> {
//                                setIconUI(IconState.DONE, false);
                                updateMessage("In sync as of " + LocalTime.now().format(TimeInput.timeFormat));
                                return SyncState.DONE;
                            }
                            case UPLOAD -> {
//                                setIconUI(IconState.UPLOAD, false);
                                updateMessage("Ahead of remote");
                                return SyncState.UPLOAD;
                            }
                            case DOWNLOAD -> {
//                                setIconUI(IconState.DOWNLOAD, false);
                                updateMessage("Behind remote");
                                return SyncState.DOWNLOAD;
                            }
                            case MERGE, CONFLICT -> {
//                                setIconUI(IconState.CONFLICT, false);
                                updateMessage("local/remote conflict (press sync to resolve)");
                                return SyncState.CONFLICT;
                            }
                            case null, default -> {
                                updateMessage("Sync not completed, unknown state: " + commitDiff.action());
                                return SyncState.ERROR;
                            }
                        }
                    }
                }
                else {
                    updateMessage("Data saved to file");
//                    setIconUI(IconState.DONE, false);
                    return SyncState.DONE;
                }

//                setDoneUI();
//                return SyncState.DONE;
            }

            @Override
            protected void failed() {
                super.failed();
                System.out.println("Failed task");
                System.out.println(getException().getMessage());
                updateMessage("Sync failed: " + getException().getMessage());
                updateValue(SyncState.ERROR);
//                setDoneUI();
//                Platform.runLater(() -> controller.syncButton.setText("Sync failed"));
            }

            @Override
            protected void cancelled() {
                super.cancelled();
                System.out.println("Cancelled task");
                updateMessage("Sync cancelled");
                updateValue(SyncState.ERROR);
//                setDoneUI();
//                Platform.runLater(() -> controller.syncButton.setText("Sync cancelled"));
            }

//            private void setStartUI() {
//                Platform.runLater(() -> {
//                    controller.syncButton.textProperty().bind(this.messageProperty());
//                    controller.syncButton.setDisable(true);
//                    setIconUI(IconState.SAVE, true);
//                });
//            }
//
//            private void setIconUI(IconState icon, boolean playAnimation) {
//                Platform.runLater(() -> {
//                    iconState.stopAnimation();
//                    iconState = icon;
//                    controller.syncButton.setGraphic(icon.icon);
//                    if (playAnimation) {
//                        icon.startAnimation();
//                    }
//                });
//            }
//
//            private void setDoneUI() {
//                Platform.runLater(() -> {
//                    controller.syncButton.textProperty().unbind();
//                    iconState.stopAnimation();
//                    controller.syncButton.setDisable(false);
//                });
//            }
        };
    }
}
