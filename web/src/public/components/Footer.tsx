export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="footer-mark">Zac Blog</div>
      <span>Copyright © 2023-{new Date().getFullYear()} Zac. Essays, notes, and code.</span>
    </footer>
  )
}
