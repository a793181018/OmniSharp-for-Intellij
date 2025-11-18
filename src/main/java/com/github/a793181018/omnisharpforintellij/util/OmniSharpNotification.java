package com.github.a793181018.omnisharpforintellij.util;

import com.github.a793181018.omnisharpforintellij.OmniSharpPlugin;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import java.util.function.BiConsumer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * OmniSharp通知工具类，用于向用户显示各种类型的通知消息
 */
public final class OmniSharpNotification {
    
    private static final String NOTIFICATION_GROUP_ID = "OmniSharp Notification Group";
    private static final NotificationGroup NOTIFICATION_GROUP = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID);
    
    private OmniSharpNotification() {
        // 私有构造函数，防止实例化
    }
    
    /**
     * 显示信息通知
     */
    public static void showInfo(@NotNull Project project, @NotNull String title, @NotNull String content) {
        showNotification(project, title, content, NotificationType.INFORMATION);
    }
    
    /**
     * 显示警告通知
     */
    public static void showWarning(@NotNull Project project, @NotNull String title, @NotNull String content) {
        showNotification(project, title, content, NotificationType.WARNING);
    }
    
    /**
     * 显示错误通知
     */
    public static void showError(@NotNull Project project, @NotNull String title, @NotNull String content) {
        showNotification(project, title, content, NotificationType.ERROR);
    }
    
    /**
     * 显示带操作按钮的通知
     */
    public static void showNotificationWithAction(
            @NotNull Project project,
            @NotNull String title,
            @NotNull String content,
            @NotNull NotificationType type,
            @NotNull @NlsActions.ActionText String actionText,
            @NotNull NotificationAction action) {
        
        if (project.isDisposed()) {
            return;
        }
        
        Notification notification = NOTIFICATION_GROUP.createNotification(
                title,
                content,
                type,
                null
        );
        
        notification.addAction(action);
        notification.notify(project);
    }
    
    /**
     * 显示通知
     */
    private static void showNotification(
            @NotNull Project project,
            @NotNull String title,
            @NotNull String content,
            @NotNull NotificationType type) {
        
        if (project.isDisposed()) {
            return;
        }
        
        Notification notification = NOTIFICATION_GROUP.createNotification(
                title,
                content,
                type,
                null
        );
        
        notification.notify(project);
    }
    
    /**
     * 显示信息对话框
     */
    public static void showInfoDialog(@Nullable Project project, @NotNull String title, @NotNull String message) {
        Messages.showInfoMessage(project, message, title);
    }
    
    /**
     * 显示警告对话框
     */
    public static void showWarningDialog(@Nullable Project project, @NotNull String title, @NotNull String message) {
        Messages.showWarningDialog(project, message, title);
    }
    
    /**
     * 显示错误对话框
     */
    public static void showErrorDialog(@Nullable Project project, @NotNull String title, @NotNull String message) {
        Messages.showErrorDialog(project, message, title);
    }
    
    /**
     * 显示确认对话框
     */
    public static boolean showConfirmDialog(@Nullable Project project, @NotNull String title, @NotNull String message) {
        return Messages.showYesNoDialog(project, message, title, Messages.getQuestionIcon()) == Messages.YES;
    }
    
    /**
     * 显示带选项的对话框
     */
    public static int showOptionsDialog(
            @Nullable Project project,
            @NotNull String title,
            @NotNull String message,
            @NotNull String[] options) {
        return Messages.showChooseDialog(message, title, options, null, Messages.getInformationIcon());
    }
    
    /**
     * 显示输入对话框
     */
    @Nullable
    public static String showInputDialog(
            @Nullable Project project,
            @NotNull String title,
            @NotNull String message,
            @Nullable String defaultValue) {
        return Messages.showInputDialog(project, message, title, null, defaultValue, null);
    }
    
    /**
     * 在EDT线程上显示通知
     */
    public static void showNotificationOnEdt(
            @NotNull Project project,
            @NotNull String title,
            @NotNull String content,
            @NotNull NotificationType type) {
        
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) {
                showNotification(project, title, content, type);
            }
        });
    }
    
    /**
     * 创建一个简单的通知操作
     */
    @NotNull
    public static NotificationAction createAction(
            @NotNull @NlsActions.ActionText String text,
            @NotNull BiConsumer<AnActionEvent, Notification> action) {
        return NotificationAction.create(text, action);
    }
    
    /**
     * 创建一个可运行的通知操作
     */
    @NotNull
    public static NotificationAction createAction(
            @NotNull @NlsActions.ActionText String text,
            @NotNull Runnable runnable) {
        return NotificationAction.create(text, (e, notification) -> runnable.run());
    }
}