import { useRef, useState, type DragEvent, type FormEvent } from 'react'
import { Link } from 'react-router'
import { toast } from 'sonner'
import { FileArchive, UploadCloud } from 'lucide-react'
import { Button, Card, CardBody, CardHeader, ErrorNote, Field, Input, PageHeader, Textarea } from '../components/ui'
import { SignInRequired } from '../components/SignInRequired'
import { useAuth } from '../lib/auth'
import { cn } from '../lib/cn'
import { usePublishService } from '../lib/queries'

export function PublishPage() {
  const { agent } = useAuth()
  const publish = usePublishService()
  const [jar, setJar] = useState<File>()
  const [dragging, setDragging] = useState(false)
  const fileInput = useRef<HTMLInputElement>(null)
  const formRef = useRef<HTMLFormElement>(null)

  function pick(file?: File) {
    if (!file) return
    if (!file.name.endsWith('.jar')) {
      toast.error('Please choose a .jar file')
      return
    }
    setJar(file)
  }

  function onDrop(e: DragEvent) {
    e.preventDefault()
    setDragging(false)
    pick(e.dataTransfer.files[0])
  }

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (!jar) return
    const data = new FormData(e.currentTarget)
    const field = (k: string) => String(data.get(k) ?? '').trim()
    const supplement = {
      class: field('class')
        .split(',')
        .map((c) => c.trim())
        .filter(Boolean)
        .join(','),
      name: field('name'),
      description: field('description'),
      vcsUrl: field('vcsUrl'),
      frontendUrl: field('frontendUrl'),
    }
    try {
      await publish.mutateAsync({ jar, supplement })
      toast.success(`Published ${supplement.name || jar.name}`, { description: 'It is now listed in the registry.' })
      formRef.current?.reset()
      setJar(undefined)
    } catch {
      // shown below the form
    }
  }

  return (
    <>
      <PageHeader
        title="Publish a service"
        description="Upload a service package. It is stored in the network, registered on the blockchain under your name, and can then be started by any node."
      />
      {!agent ? (
        <SignInRequired what="publish services under your name" />
      ) : (
        <div className="grid gap-6 lg:grid-cols-[1fr_18rem]">
          <Card>
            <form ref={formRef} onSubmit={submit}>
              <CardHeader title="Service package" description="The jar must declare Library-SymbolicName and Library-Version in its manifest." />
              <CardBody className="space-y-5">
                <div
                  onDragOver={(e) => {
                    e.preventDefault()
                    setDragging(true)
                  }}
                  onDragLeave={() => setDragging(false)}
                  onDrop={onDrop}
                  onClick={() => fileInput.current?.click()}
                  onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && fileInput.current?.click()}
                  role="button"
                  tabIndex={0}
                  aria-label="Choose service jar"
                  className={cn(
                    'flex cursor-pointer flex-col items-center rounded-xl border-2 border-dashed px-6 py-8 text-center transition-colors',
                    dragging
                      ? 'border-brand-500 bg-brand-50 dark:bg-brand-500/10'
                      : 'border-zinc-300 hover:border-zinc-400 dark:border-zinc-700 dark:hover:border-zinc-600',
                  )}
                >
                  {jar ? (
                    <>
                      <FileArchive className="size-8 text-brand-600" />
                      <p className="mt-2 font-medium">{jar.name}</p>
                      <p className="text-xs text-zinc-500">{(jar.size / 1024).toFixed(1)} KB · click to replace</p>
                    </>
                  ) : (
                    <>
                      <UploadCloud className="size-8 text-zinc-400" />
                      <p className="mt-2 font-medium">Drop your service jar here</p>
                      <p className="text-xs text-zinc-500">or click to browse</p>
                    </>
                  )}
                  <input ref={fileInput} type="file" accept=".jar" className="hidden" onChange={(e) => pick(e.target.files?.[0])} />
                </div>

                <div className="grid gap-5 sm:grid-cols-2">
                  <Field label="Display name">
                    <Input name="name" placeholder="My Service" />
                  </Field>
                  <Field label="Service classes" hint="Comma-separated class names, e.g. MyService">
                    <Input name="class" placeholder="MyService" required />
                  </Field>
                </div>
                <Field label="Description">
                  <Textarea name="description" rows={3} placeholder="What does the service do?" />
                </Field>
                <div className="grid gap-5 sm:grid-cols-2">
                  <Field label="Source code URL">
                    <Input name="vcsUrl" type="url" placeholder="https://github.com/…" />
                  </Field>
                  <Field label="Frontend URL" hint="Shown as “Open app” when the service runs">
                    <Input name="frontendUrl" type="url" placeholder="https://…" />
                  </Field>
                </div>
                <ErrorNote error={publish.error} />
                <div className="flex justify-end">
                  <Button type="submit" disabled={!jar} loading={publish.isPending}>
                    Publish service
                  </Button>
                </div>
              </CardBody>
            </form>
          </Card>
          <aside className="space-y-4 text-sm text-zinc-600 dark:text-zinc-400">
            <Card>
              <CardBody className="space-y-2">
                <h3 className="font-semibold text-zinc-900 dark:text-zinc-100">How publishing works</h3>
                <p>The package is split into artifacts and stored, signed by you, in the shared network storage.</p>
                <p>The release is registered on the blockchain with you as author, so others can verify where it comes from.</p>
                <p>
                  New versions need a higher version number. Then start it from <Link to="/services" className="text-brand-600 hover:underline">Services</Link>.
                </p>
              </CardBody>
            </Card>
          </aside>
        </div>
      )}
    </>
  )
}
