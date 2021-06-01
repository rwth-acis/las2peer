import { PaperToastElement } from '@polymer/paper-toast/paper-toast';

export function showNotificationToast() {
  const notificationContainer = document.getElementById(`notification`)!;

  const toast = notificationContainer.shadowRoot!.getElementById(
    'notification-toast'
  )! as PaperToastElement;

  toast.open();
}
