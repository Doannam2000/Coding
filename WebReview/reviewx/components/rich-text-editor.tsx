"use client";

import { useEffect, useMemo, useRef } from "react";

type RichTextEditorProps = {
  label: string;
  value: string;
  onChange: (next: string) => void;
  placeholder?: string;
};

type CommandButton = {
  key: string;
  label: string;
  command: "bold" | "italic" | "insertUnorderedList" | "insertOrderedList";
};

const COMMAND_BUTTONS: CommandButton[] = [
  { key: "bold", label: "B", command: "bold" },
  { key: "italic", label: "I", command: "italic" },
  { key: "bullet", label: "• List", command: "insertUnorderedList" },
  { key: "numbered", label: "1. List", command: "insertOrderedList" },
];

function normalizeEmptyHtml(input: string): string {
  const trimmed = input.trim();
  return trimmed === "<br>" || trimmed === "<div><br></div>" ? "" : trimmed;
}

export function RichTextEditor({ label, value, onChange, placeholder }: RichTextEditorProps) {
  const editorRef = useRef<HTMLDivElement | null>(null);
  const cleanedValue = useMemo(() => normalizeEmptyHtml(value), [value]);

  useEffect(() => {
    const node = editorRef.current;
    if (!node) return;
    const normalizedCurrent = normalizeEmptyHtml(node.innerHTML);
    if (normalizedCurrent !== cleanedValue) {
      node.innerHTML = cleanedValue;
    }
  }, [cleanedValue]);

  function runCommand(command: CommandButton["command"]) {
    editorRef.current?.focus();
    document.execCommand(command);
    const next = normalizeEmptyHtml(editorRef.current?.innerHTML ?? "");
    onChange(next);
  }

  function insertLink() {
    const url = window.prompt("Nhập URL:");
    if (!url) return;
    editorRef.current?.focus();
    document.execCommand("createLink", false, url.trim());
    const next = normalizeEmptyHtml(editorRef.current?.innerHTML ?? "");
    onChange(next);
  }

  return (
    <div className="space-y-1 text-sm">
      <span className="font-semibold text-slate-800">{label}</span>
      <div className="rounded-xl border border-slate-200 bg-white">
        <div className="flex flex-wrap items-center gap-2 border-b border-slate-200 px-2 py-2">
          {COMMAND_BUTTONS.map((button) => (
            <button
              key={button.key}
              type="button"
              onClick={() => runCommand(button.command)}
              className="rounded-md border border-slate-200 bg-slate-50 px-2 py-1 text-xs font-semibold text-slate-700 hover:bg-slate-100"
            >
              {button.label}
            </button>
          ))}
          <button
            type="button"
            onClick={insertLink}
            className="rounded-md border border-slate-200 bg-slate-50 px-2 py-1 text-xs font-semibold text-slate-700 hover:bg-slate-100"
          >
            Link
          </button>
        </div>
        <div className="relative">
          {!cleanedValue && placeholder ? (
            <span className="pointer-events-none absolute left-3 top-3 text-sm text-slate-400">{placeholder}</span>
          ) : null}
          <div
            ref={editorRef}
            contentEditable
            onInput={(e) => onChange(normalizeEmptyHtml(e.currentTarget.innerHTML))}
            className="min-h-40 px-3 py-3 text-sm text-slate-800 outline-none"
            suppressContentEditableWarning
          />
        </div>
      </div>
    </div>
  );
}
