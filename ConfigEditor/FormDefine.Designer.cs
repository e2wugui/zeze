
namespace ConfigEditor
{
    partial class FormDefine
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.components = new System.ComponentModel.Container();
            this.define = new System.Windows.Forms.DataGridView();
            this.BeanLocked = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.VarName = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.VarType = new System.Windows.Forms.DataGridViewComboBoxColumn();
            this.VarValue = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.VarForeign = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.VarProperties = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.VarComment = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.contextMenuStrip1 = new System.Windows.Forms.ContextMenuStrip(this.components);
            this.deleteVariableColumnToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            ((System.ComponentModel.ISupportInitialize)(this.define)).BeginInit();
            this.contextMenuStrip1.SuspendLayout();
            this.SuspendLayout();
            // 
            // define
            // 
            this.define.AllowDrop = true;
            this.define.AllowUserToAddRows = false;
            this.define.AllowUserToDeleteRows = false;
            this.define.AllowUserToResizeRows = false;
            this.define.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom) 
            | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.define.ColumnHeadersHeight = 20;
            this.define.ColumnHeadersHeightSizeMode = System.Windows.Forms.DataGridViewColumnHeadersHeightSizeMode.DisableResizing;
            this.define.Columns.AddRange(new System.Windows.Forms.DataGridViewColumn[] {
            this.BeanLocked,
            this.VarName,
            this.VarType,
            this.VarValue,
            this.VarForeign,
            this.VarProperties,
            this.VarComment});
            this.define.Location = new System.Drawing.Point(-5, -1);
            this.define.Margin = new System.Windows.Forms.Padding(2);
            this.define.MultiSelect = false;
            this.define.Name = "define";
            this.define.RowHeadersWidth = 25;
            this.define.RowTemplate.Height = 18;
            this.define.SelectionMode = System.Windows.Forms.DataGridViewSelectionMode.CellSelect;
            this.define.Size = new System.Drawing.Size(989, 504);
            this.define.TabIndex = 0;
            this.define.CellEndEdit += new System.Windows.Forms.DataGridViewCellEventHandler(this.define_CellEndEdit);
            this.define.CellMouseDoubleClick += new System.Windows.Forms.DataGridViewCellMouseEventHandler(this.define_CellMouseDoubleClick);
            this.define.CellMouseDown += new System.Windows.Forms.DataGridViewCellMouseEventHandler(this.define_CellMouseDown);
            this.define.CellMouseMove += new System.Windows.Forms.DataGridViewCellMouseEventHandler(this.define_CellMouseMove);
            this.define.CellValidating += new System.Windows.Forms.DataGridViewCellValidatingEventHandler(this.define_CellValidating);
            this.define.CellValueChanged += new System.Windows.Forms.DataGridViewCellEventHandler(this.define_CellValueChanged);
            this.define.DragDrop += new System.Windows.Forms.DragEventHandler(this.define_DragDrop);
            this.define.DragEnter += new System.Windows.Forms.DragEventHandler(this.define_DragEnter);
            this.define.DragOver += new System.Windows.Forms.DragEventHandler(this.define_DragOver);
            // 
            // BeanLocked
            // 
            this.BeanLocked.HeaderText = "锁定";
            this.BeanLocked.Name = "BeanLocked";
            this.BeanLocked.Resizable = System.Windows.Forms.DataGridViewTriState.True;
            this.BeanLocked.SortMode = System.Windows.Forms.DataGridViewColumnSortMode.NotSortable;
            this.BeanLocked.ToolTipText = "锁定Bean，不能再增删变量（数据列）";
            this.BeanLocked.Width = 40;
            // 
            // VarName
            // 
            this.VarName.HeaderText = "名字";
            this.VarName.MinimumWidth = 10;
            this.VarName.Name = "VarName";
            this.VarName.SortMode = System.Windows.Forms.DataGridViewColumnSortMode.NotSortable;
            this.VarName.ToolTipText = "列（变量）名，也用来显示Bean的名字。";
            // 
            // VarType
            // 
            this.VarType.DisplayStyle = System.Windows.Forms.DataGridViewComboBoxDisplayStyle.Nothing;
            this.VarType.FlatStyle = System.Windows.Forms.FlatStyle.Flat;
            this.VarType.HeaderText = "类型";
            this.VarType.MinimumWidth = 10;
            this.VarType.Name = "VarType";
            this.VarType.ToolTipText = "数据类型（主要为了程序）";
            this.VarType.Width = 80;
            // 
            // VarValue
            // 
            this.VarValue.HeaderText = "引用";
            this.VarValue.MinimumWidth = 10;
            this.VarValue.Name = "VarValue";
            this.VarValue.SortMode = System.Windows.Forms.DataGridViewColumnSortMode.NotSortable;
            this.VarValue.ToolTipText = "如果此列类型为List，这里是它的item的类型名字（BeanFullName）";
            this.VarValue.Width = 150;
            // 
            // VarForeign
            // 
            this.VarForeign.HeaderText = "Foreign";
            this.VarForeign.MinimumWidth = 10;
            this.VarForeign.Name = "VarForeign";
            this.VarForeign.SortMode = System.Windows.Forms.DataGridViewColumnSortMode.NotSortable;
            this.VarForeign.ToolTipText = "此列的值必须在另一个配置中存在。格式如：BeanFullName:VarName";
            this.VarForeign.Width = 150;
            // 
            // VarProperties
            // 
            this.VarProperties.HeaderText = "属性";
            this.VarProperties.MinimumWidth = 10;
            this.VarProperties.Name = "VarProperties";
            this.VarProperties.ReadOnly = true;
            this.VarProperties.Resizable = System.Windows.Forms.DataGridViewTriState.True;
            this.VarProperties.SortMode = System.Windows.Forms.DataGridViewColumnSortMode.NotSortable;
            this.VarProperties.ToolTipText = "各种扩展属性，详细见属性编辑窗口。";
            this.VarProperties.Width = 200;
            // 
            // VarComment
            // 
            this.VarComment.HeaderText = "注释";
            this.VarComment.MinimumWidth = 10;
            this.VarComment.Name = "VarComment";
            this.VarComment.SortMode = System.Windows.Forms.DataGridViewColumnSortMode.NotSortable;
            this.VarComment.ToolTipText = "注释";
            this.VarComment.Width = 200;
            // 
            // contextMenuStrip1
            // 
            this.contextMenuStrip1.Items.AddRange(new System.Windows.Forms.ToolStripItem[] {
            this.deleteVariableColumnToolStripMenuItem});
            this.contextMenuStrip1.Name = "contextMenuStrip1";
            this.contextMenuStrip1.Size = new System.Drawing.Size(259, 26);
            this.contextMenuStrip1.Text = "&Delete Variable(Column)";
            // 
            // deleteVariableColumnToolStripMenuItem
            // 
            this.deleteVariableColumnToolStripMenuItem.Name = "deleteVariableColumnToolStripMenuItem";
            this.deleteVariableColumnToolStripMenuItem.Size = new System.Drawing.Size(258, 22);
            this.deleteVariableColumnToolStripMenuItem.Text = "&Delete Bean Variable(Data Column)";
            this.deleteVariableColumnToolStripMenuItem.Click += new System.EventHandler(this.deleteVariableColumnToolStripMenuItem_Click);
            // 
            // FormDefine
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 12F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(983, 505);
            this.Controls.Add(this.define);
            this.Margin = new System.Windows.Forms.Padding(2);
            this.Name = "FormDefine";
            this.ShowInTaskbar = false;
            this.StartPosition = System.Windows.Forms.FormStartPosition.CenterParent;
            this.Text = "Bean 结构定义";
            this.FormClosing += new System.Windows.Forms.FormClosingEventHandler(this.FormDefine_FormClosing);
            ((System.ComponentModel.ISupportInitialize)(this.define)).EndInit();
            this.contextMenuStrip1.ResumeLayout(false);
            this.ResumeLayout(false);

        }

        #endregion

        private System.Windows.Forms.DataGridView define;
        private System.Windows.Forms.ContextMenuStrip contextMenuStrip1;
        private System.Windows.Forms.ToolStripMenuItem deleteVariableColumnToolStripMenuItem;
        private System.Windows.Forms.DataGridViewTextBoxColumn BeanLocked;
        private System.Windows.Forms.DataGridViewTextBoxColumn VarName;
        private System.Windows.Forms.DataGridViewComboBoxColumn VarType;
        private System.Windows.Forms.DataGridViewTextBoxColumn VarValue;
        private System.Windows.Forms.DataGridViewTextBoxColumn VarForeign;
        private System.Windows.Forms.DataGridViewTextBoxColumn VarProperties;
        private System.Windows.Forms.DataGridViewTextBoxColumn VarComment;
    }
}