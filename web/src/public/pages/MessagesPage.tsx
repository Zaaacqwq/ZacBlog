import CommentSection from '../components/CommentSection'
import { TwoColumn } from '../components/PublicLayout'
import Sidebar from '../components/Sidebar'

/** 留言板（旧 cmsMessage.vue） */
export default function MessagesPage() {
  return (
    <div className="page">
      <TwoColumn side={<Sidebar />}>
        <CommentSection target={{ kind: 'message' }} formPlacement="separate" />
      </TwoColumn>
    </div>
  )
}
