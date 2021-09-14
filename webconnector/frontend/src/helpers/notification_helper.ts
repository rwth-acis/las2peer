import { PaperToastElement } from '@polymer/paper-toast/paper-toast';

export function showNotificationToast(message: string) {
  const notificationContainer = document.getElementById(`notification`)!;
  console.log(notificationContainer);
  const toast = notificationContainer.shadowRoot!.getElementById(
    'notification-toast'
  )! as PaperToastElement;
  toast.text = message;
  toast.open();
}
